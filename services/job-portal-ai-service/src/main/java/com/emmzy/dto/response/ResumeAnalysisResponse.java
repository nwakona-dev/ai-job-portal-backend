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
public class ResumeAnalysisResponse {

    private Double overallScore;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> recommendedSkills;
    private String summary;
}

