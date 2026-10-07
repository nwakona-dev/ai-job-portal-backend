package com.emmzy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OpenAIChatRequest {

    @NotBlank(message = "Message is required")
    private String message;
}

