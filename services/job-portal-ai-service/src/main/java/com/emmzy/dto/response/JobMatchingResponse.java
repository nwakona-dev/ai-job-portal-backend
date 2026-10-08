package com.emmzy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchingResponse {

    private Long jobId;
    private Long candidateId;
    private Double matchScore;
    private String matchLevel;
    private List<String> matchingSkills;
    private List<String> missingSkills;
    private String explanation;
}
