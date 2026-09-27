package com.emmzy.dto;

import com.emmzy.domain.HiringDecision;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HiringDecisionRequest {

    @NotNull(message = "Hiring decision is required")
    private HiringDecision hiringDecision;
}