package com.emmzy.dto;

import lombok.*;

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

    private Double expectedSalary;

    private String availableFrom;
}