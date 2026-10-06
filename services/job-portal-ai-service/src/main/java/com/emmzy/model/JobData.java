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
public class JobData {

    private Long id;

    private String title;

    private String description;

    private String category;

    private String jobType;

    private String workMode;

    private String experienceLevel;

    private String city;

    private String country;

    private Integer openings;

    private Long employerId;

    private String companyName;

    private List<SkillData> skills;
}
