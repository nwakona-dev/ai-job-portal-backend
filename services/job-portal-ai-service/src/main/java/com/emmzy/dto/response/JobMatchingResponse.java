 package com.emmzy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchingResponse {

    private Long jobId;
    private Long candidateId;
    private Double matchScore;
    private String matchLevel;
    private String explanation;
}

