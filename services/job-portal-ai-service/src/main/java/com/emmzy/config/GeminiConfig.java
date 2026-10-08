package com.emmzy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "gemini")
public class GeminiConfig {

    private String apiKey;

    private String model = "gemini-3.8-flash";

    private String baseUrl = "https://generativelanguage.googleapis.com";
}