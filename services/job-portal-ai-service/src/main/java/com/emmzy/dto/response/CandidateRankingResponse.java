package com.emmzy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateRankingResponse {

    private Long candidateId;
    private Integer rank;
    private Double matchScore;
    private String explanation;
}
