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
public class SkillMatchResponse {

    private Long candidateId;
    private List<String> matchingSkills;
    private List<String> missingSkills;
    private Double matchScore;
}

