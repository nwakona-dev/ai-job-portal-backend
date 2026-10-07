package com.emmzy.service;

import com.emmzy.config.OpenAIConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenAIService {

    private final OpenAIConfig openAIConfig;
    private final RestClient openAIRestClient;
    private final ObjectMapper objectMapper;

    public String generateResponse(String prompt) {

        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("OpenAI prompt cannot be empty");
        }

        Map<String, Object> request = new HashMap<>();

        request.put("model", openAIConfig.getModel());
        request.put("input", prompt);

        String response = openAIRestClient
                .post()
                .uri("/v1/responses")
                .body(request)
                .retrieve()
                .body(String.class);

        return extractOutputText(response);
    }

    private String extractOutputText(String response) {

        try {
            JsonNode root = objectMapper.readTree(response);

            JsonNode output = root.path("output");

            if (!output.isArray()) {
                throw new IllegalStateException(
                        "OpenAI response did not contain an output array"
                );
            }

            StringBuilder result = new StringBuilder();

            for (JsonNode outputItem : output) {

                JsonNode content = outputItem.path("content");

                if (!content.isArray()) {
                    continue;
                }

                for (JsonNode contentItem : content) {

                    String type = contentItem
                            .path("type")
                            .asText();

                    if ("output_text".equals(type)) {

                        String text = contentItem
                                .path("text")
                                .asText();

                        if (!text.isBlank()) {

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
                        "OpenAI returned no text output"
                );
            }

            return result.toString();

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Failed to parse OpenAI response: "
                            + exception.getMessage(),
                    exception
            );
        }
    }
}

