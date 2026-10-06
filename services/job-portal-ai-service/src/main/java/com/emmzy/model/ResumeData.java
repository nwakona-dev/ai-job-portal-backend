package com.emmzy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeData {

    private Long id;

    private Long candidateId;

    private String title;

    private String summary;

    private String experience;

    private String education;

    private List<SkillData> skills;
}

