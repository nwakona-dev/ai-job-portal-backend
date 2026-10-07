package com.emmzy.service;

import com.emmzy.config.OpenAIConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenAIService {

    private final OpenAIConfig openAIConfig;
    private final RestClient openAIRestClient;
    private final ObjectMapper objectMapper;

    public String generateResponse(String prompt) {

        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException(
                    "OpenAI prompt cannot be empty."
            );
        }

        if (openAIConfig.getApiKey() == null
                || openAIConfig.getApiKey().isBlank()) {

            throw new IllegalStateException(
                    "OPENAI_API_KEY is not configured."
            );
        }

        if (openAIConfig.getModel() == null
                || openAIConfig.getModel().isBlank()) {

            throw new IllegalStateException(
                    "OpenAI model is not configured."
            );
        }

        Map<String, Object> request =
                new LinkedHashMap<>();

        request.put(
                "model",
                openAIConfig.getModel()
        );

        request.put(
                "input",
                prompt.trim()
        );

        try {

            String response = openAIRestClient
                    .post()
                    .uri("/v1/responses")
                    .body(request)
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            (requestObject, responseObject) -> {

                                String errorBody;

                                try {
                                    errorBody =
                                            new String(
                                                    responseObject
                                                            .getBody()
                                                            .readAllBytes()
                                            );
                                } catch (Exception exception) {
                                    errorBody =
                                            "Unable to read OpenAI error response.";
                                }

                                throw new IllegalStateException(
                                        "OpenAI API returned HTTP "
                                                + responseObject
                                                .getStatusCode()
                                                .value()
                                                + ": "
                                                + extractErrorMessage(
                                                errorBody
                                        )
                                );
                            }
                    )
                    .body(String.class);

            if (response == null || response.isBlank()) {
                throw new IllegalStateException(
                        "OpenAI returned an empty response."
                );
            }

            return extractOutputText(response);

        } catch (RestClientException exception) {

            throw new IllegalStateException(
                    "Could not connect to OpenAI: "
                            + exception.getMessage(),
                    exception
            );
        }
    }

    private String extractOutputText(
            String response
    ) {

        try {

            JsonNode root =
                    objectMapper.readTree(response);

            /*
             * Responses API normally returns:
             *
             * {
             *   "output": [
             *     {
             *       "content": [
             *         {
             *           "type": "output_text",
             *           "text": "..."
             *         }
             *       ]
             *     }
             *   ]
             * }
             */

            JsonNode output =
                    root.path("output");

            if (!output.isArray()) {

                String apiError =
                        extractErrorMessage(response);

                throw new IllegalStateException(
                        "OpenAI response did not contain an output array. "
                                + apiError
                );
            }

            StringBuilder result =
                    new StringBuilder();

            for (JsonNode outputItem : output) {

                JsonNode content =
                        outputItem.path("content");

                if (!content.isArray()) {
                    continue;
                }

                for (JsonNode contentItem : content) {

                    String type =
                            contentItem
                                    .path("type")
                                    .asText();

                    if (!"output_text".equals(type)) {
                        continue;
                    }

                    String text =
                            contentItem
                                    .path("text")
                                    .asText();

                    if (text == null || text.isBlank()) {
                        continue;
                    }

                    if (!result.isEmpty()) {
                        result.append("\n");
                    }

                    result.append(text);
                }
            }

            if (result.isEmpty()) {

                /*
                 * Some Responses API payloads can expose
                 * output_text in a different location.
                 */
                JsonNode outputText =
                        root.path("output_text");

                if (outputText.isArray()) {

                    for (JsonNode textNode : outputText) {

                        String text =
                                textNode.asText();

                        if (text != null
                                && !text.isBlank()) {

                            if (!result.isEmpty()) {
                                result.append("\n");
                            }

                            result.append(text);
                        }
                    }
                }
            }

            if (result.isEmpty()) {
                throw new IllegalStateException(
                        "OpenAI returned no text output."
                );
            }

            return result.toString().trim();

        } catch (IllegalStateException exception) {
            throw exception;

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Failed to parse OpenAI response: "
                            + exception.getMessage(),
                    exception
            );
        }
    }

    private String extractErrorMessage(
            String response
    ) {

        if (response == null || response.isBlank()) {
            return "No error details were returned.";
        }

        try {

            JsonNode root =
                    objectMapper.readTree(response);

            JsonNode error =
                    root.path("error");

            if (!error.isMissingNode()) {

                String message =
                        error.path("message").asText();

                if (message != null
                        && !message.isBlank()) {

                    return message;
                }

                String type =
                        error.path("type").asText();

                if (type != null
                        && !type.isBlank()) {

                    return type;
                }
            }

        } catch (Exception ignored) {
            // Fall back to raw response below.
        }

        return response.length() > 1000
                ? response.substring(0, 1000)
                : response;
    }
}

