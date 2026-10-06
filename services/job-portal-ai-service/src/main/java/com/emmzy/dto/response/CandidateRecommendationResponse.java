package com.emmzy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateRecommendationResponse {

    private Long candidateId;
    private String candidateName;
    private Double matchScore;
    private String explanation;
}

