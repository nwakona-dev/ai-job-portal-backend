package com.emmzy.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchAIResult {

    private double matchScore;

    private List<String> matchingSkills;

    private List<String> missingSkills;

    private String explanation;
}