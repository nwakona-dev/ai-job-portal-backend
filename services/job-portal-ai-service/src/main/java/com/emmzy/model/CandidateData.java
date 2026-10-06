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
public class CandidateData {

    private Long id;

    private String fullName;

    private String email;

    private String phone;

    private List<SkillData> skills;

    private String experience;

    private String education;
}

