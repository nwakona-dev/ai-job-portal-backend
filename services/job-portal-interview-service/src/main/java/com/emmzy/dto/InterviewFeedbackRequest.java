package com.emmzy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewFeedbackRequest {

    @NotBlank(message = "Feedback is required")
    private String feedback;
}