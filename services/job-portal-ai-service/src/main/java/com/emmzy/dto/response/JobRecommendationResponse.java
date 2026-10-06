package com.emmzy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRecommendationResponse {

    private Long jobId;
    private String title;
    private String companyName;
    private Double matchScore;
    private String explanation;
}

