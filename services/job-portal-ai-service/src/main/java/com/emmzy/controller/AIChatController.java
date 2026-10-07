package com.emmzy.controller;

import com.emmzy.dto.request.OpenAIChatRequest;
import com.emmzy.dto.response.OpenAIChatResponse;
import com.emmzy.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIChatController {

    private final OpenAIService openAIService;

    /**
     * Send a message to the OpenAI GPT model.
     *
     * POST /api/ai/chat
     */
    @PostMapping("/chat")
    public ResponseEntity<OpenAIChatResponse> chat(
            @RequestBody OpenAIChatRequest request
    ) {

        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }

        String response = openAIService.generateResponse(
                request.getMessage()
        );

        OpenAIChatResponse chatResponse =
                new OpenAIChatResponse(response);

        return ResponseEntity.ok(chatResponse);
    }
}