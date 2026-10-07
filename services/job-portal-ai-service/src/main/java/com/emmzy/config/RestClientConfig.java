package com.emmzy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient openAIRestClient(
            OpenAIConfig openAIConfig
    ) {

        if (openAIConfig.getApiKey() == null
                || openAIConfig.getApiKey().isBlank()) {

            throw new IllegalStateException(
                    "OpenAI API key is not configured. " +
                            "Set the OPENAI_API_KEY environment variable."
            );
        }

        return RestClient.builder()
                .baseUrl(openAIConfig.getBaseUrl())
                .defaultHeader(
                        "Authorization",
                        "Bearer " + openAIConfig.getApiKey()
                )
                .defaultHeader(
                        "Content-Type",
                        "application/json"
                )
                .build();
    }
}