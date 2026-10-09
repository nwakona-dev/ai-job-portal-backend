
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

        String apiKey = openAIConfig.getApiKey();
        String model = openAIConfig.getModel();

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OPENAI_API_KEY is not configured."
            );
        }

        if (model == null || model.isBlank()) {
            throw new IllegalStateException(
                    "OpenAI model is not configured."
            );
        }

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("input", prompt.trim());

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
                                    errorBody = new String(
                                            responseObject.getBody()
                                                    .readAllBytes(),
                                            java.nio.charset.StandardCharsets.UTF_8
                                    );
                                } catch (Exception exception) {
                                    errorBody =
                                            "Unable to read OpenAI error response.";
                                }

                                throw new IllegalStateException(
                                        "OpenAI API returned HTTP "
                                                + responseObject.getStatusCode()
                                                .value()
                                                + ": "
                                                + extractErrorMessage(errorBody)
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
                    "Could not connect to OpenAI. Check the network, "
                            + "base URL, and API configuration. Details: "
                            + exception.getMessage(),
                    exception
            );
        }
    }

    private String extractOutputText(String response) {

        try {
            JsonNode root = objectMapper.readTree(response);

            JsonNode output = root.path("output");
            StringBuilder result = new StringBuilder();

            if (output.isArray()) {
                for (JsonNode outputItem : output) {
                    JsonNode content = outputItem.path("content");

                    if (!content.isArray()) {
                        continue;
                    }

                    for (JsonNode contentItem : content) {
                        String type = contentItem.path("type").asText();

                        if ("output_text".equals(type)) {
                            appendText(
                                    result,
                                    contentItem.path("text").asText()
                            );
                        }
                    }
                }
            }

            // Responses API may also provide output_text at the root.
            if (result.isEmpty()) {
                JsonNode outputText = root.path("output_text");

                if (outputText.isArray()) {
                    for (JsonNode textNode : outputText) {
                        appendText(result, textNode.asText());
                    }
                } else if (!outputText.isMissingNode()
                        && !outputText.isNull()) {
                    appendText(result, outputText.asText());
                }
            }

            if (result.isEmpty()) {
                String status = root.path("status").asText();
                String details = extractErrorMessage(response);

                throw new IllegalStateException(
                        "OpenAI returned no text output."
                                + (status.isBlank()
                                ? ""
                                : " Response status: " + status + ".")
                                + " Details: " + details
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

    private void appendText(StringBuilder result, String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        if (!result.isEmpty()) {
            result.append('\n');
        }

        result.append(text);
    }

    private String extractErrorMessage(String response) {

        if (response == null || response.isBlank()) {
            return "No error details were returned.";
        }

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode error = root.path("error");

            if (!error.isMissingNode()) {
                String message = error.path("message").asText();

                if (!message.isBlank()) {
                    return message;
                }

                String type = error.path("type").asText();

                if (!type.isBlank()) {
                    return type;
                }

                String code = error.path("code").asText();

                if (!code.isBlank()) {
                    return code;
                }
            }
        } catch (Exception ignored) {
            // Use the response body if it is not valid JSON.
        }

        return response.length() > 1000
                ? response.substring(0, 1000)
                : response;
    }
}