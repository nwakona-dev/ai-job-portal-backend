package com.emmzy.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateInterviewRequest {

    @NotNull(message = "Interview date is required")
    @FutureOrPresent(message = "Interview date cannot be in the past")
    private LocalDate scheduledDate;

    @NotNull(message = "Interview time is required")
    private LocalTime scheduledTime;

    @NotNull(message = "Duration is required")
    @Min(value = 15, message = "Interview duration must be at least 15 minutes")
    private Integer durationMinutes;

    private String meetingLink;

    private String location;

    private String instructions;
}