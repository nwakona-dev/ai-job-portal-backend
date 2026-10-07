package com.emmzy.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AIServiceException.class)
    public ResponseEntity<Map<String, Object>> handleAIServiceException(
            AIServiceException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "success", false,
                        "error", "AI_SERVICE_ERROR",
                        "message", safeMessage(exception)
                ));
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<Map<String, Object>> handleExternalServiceException(
            ExternalServiceException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(Map.of(
                        "success", false,
                        "error", "EXTERNAL_SERVICE_ERROR",
                        "message", safeMessage(exception)
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "success", false,
                        "error", "INVALID_REQUEST",
                        "message", safeMessage(exception)
                ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(
            IllegalStateException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(Map.of(
                        "success", false,
                        "error", "AI_PROVIDER_ERROR",
                        "message", safeMessage(exception)
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(
            Exception exception
    ) {

        /*
         * Do not hide the actual exception anymore.
         *
         * This is especially important while the AI service
         * is being integrated with OpenAI.
         */

        exception.printStackTrace();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "success", false,
                        "error", "INTERNAL_SERVER_ERROR",
                        "message", safeMessage(exception)
                ));
    }

    private String safeMessage(
            Exception exception
    ) {
        if (exception == null) {
            return "Unknown server error.";
        }

        String message =
                exception.getMessage();

        if (message == null
                || message.isBlank()) {

            return exception.getClass()
                    .getSimpleName();
        }

        return message;
    }
}
