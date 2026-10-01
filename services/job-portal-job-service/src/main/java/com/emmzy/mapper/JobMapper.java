package com.emmzy.mapper;

import com.emmzy.dto.CompanyResponse;
import com.emmzy.dto.JobResponse;
import com.emmzy.dto.JobSkillResponse;
import com.emmzy.dto.JobTagResponse;
import com.emmzy.model.Job;
import com.emmzy.model.embeddable.JobLocation;
import com.emmzy.model.embeddable.SalaryRange;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class JobMapper {

    private JobMapper() {
        // Utility class
    }

    public static JobResponse toResponse(
            Job job,
            CompanyResponse companyResponse
    ) {

        if (job == null) {
            return null;
        }

        JobLocation loc = job.getLocation();
        SalaryRange sal = job.getSalaryRange();

        Set<JobSkillResponse> skills =
                job.getSkills() == null
                        ? Collections.emptySet()
                        : job.getSkills()
                        .stream()
                        .map(JobSkillMapper::toJobSkillResponse)
                        .collect(Collectors.toSet());

        Set<JobTagResponse> tags =
                job.getTags() == null
                        ? Collections.emptySet()
                        : job.getTags()
                        .stream()
                        .map(JobTagMapper::toTagResponse)
                        .collect(Collectors.toSet());

        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .responsibilities(job.getResponsibilities())
                .benefits(job.getBenefits())

                // Company
                .company(companyResponse)
                .employerId(job.getEmployerId())

                // Category
                .category(
                        job.getCategory() != null
                                ? JobCategoryMapper.toJobCategoryResponse(
                                job.getCategory(),
                                false
                        )
                                : null
                )

                // Skills and tags
                .skills(skills)
                .tags(tags)

                // Location
                .address(
                        loc != null
                                ? loc.getAddress()
                                : null
                )
                .city(
                        loc != null
                                ? loc.getCity()
                                : null
                )
                .state(
                        loc != null
                                ? loc.getState()
                                : null
                )
                .country(
                        loc != null
                                ? loc.getCountry()
                                : null
                )
                .zipCode(
                        loc != null
                                ? loc.getZipCode()
                                : null
                )

                // Salary
                .minSalary(
                        sal != null
                                ? sal.getMinSalary()
                                : null
                )
                .maxSalary(
                        sal != null
                                ? sal.getMaxSalary()
                                : null
                )

                // Job details
                .jobType(job.getJobType())
                .workMode(job.getWorkMode())
                .experienceLevel(job.getExperienceLevel())

                // Publication state
                .status(job.getStatus())
                .active(job.getActive())

                // Other details
                .openings(job.getOpenings())
                .applicationDeadline(
                        job.getApplicationDeadline()
                )
                .expiredAt(job.getExpiredAt())

                // Timestamps
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .publishedAt(job.getPublishedAt())
                .closedAt(job.getClosedAt())

                .build();
    }
}

