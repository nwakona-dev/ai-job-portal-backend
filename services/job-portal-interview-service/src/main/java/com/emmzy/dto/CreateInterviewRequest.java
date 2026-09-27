package com.emmzy.dto;

import com.emmzy.domain.InterviewType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInterviewRequest {

    @NotNull(message = "Application ID is required")
    private Long applicationId;

    @NotNull(message = "Interview date is required")
    @FutureOrPresent(message = "Interview date cannot be in the past")
    private LocalDate scheduledDate;

    @NotNull(message = "Interview time is required")
    private LocalTime scheduledTime;

    @NotNull(message = "Duration is required")
    @Min(value = 15, message = "Interview duration must be at least 15 minutes")
    private Integer durationMinutes;

    @NotNull(message = "Interview type is required")
    private InterviewType interviewType;

    @NotBlank(message = "Interview location is required")
    private String location;

    private String instructions;
}