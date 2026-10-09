
package com.emmzy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAIChatResponse {

    private boolean success;

    private String response;

    private String error;

    public OpenAIChatResponse(String response) {
        this.success = true;
        this.response = response;
        this.error = null;
    }
}