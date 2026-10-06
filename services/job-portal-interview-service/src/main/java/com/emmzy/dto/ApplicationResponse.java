package com.emmzy.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponse {

    private Long id;

    private CandidateResponse candidate;

    private Long employerId;

    private JobResponse job;

    private CompanyResponse company;

    private String status;

    private Long resumeId;

    private String coverLetter;

    private BigDecimal expectedSalary;

    private LocalDate availableFrom;
}