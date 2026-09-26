package com.emmzy.dto;

import com.emmzy.domain.ExperienceLevel;
import com.emmzy.domain.JobType;
import com.emmzy.domain.WorkMode;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRequest {
    @NotBlank(message = "Job Title is required")
    private String title;

    @NotBlank(message = "Job Description is required")
    private String description;

    private String requirements;
    private String responsibilities;
    private String benefits;

    @NotNull(message = "Category is required")
    private Long categoryId;

    private Set<Long> skillIds;

    private Set<Long> tagIds;

    private String address;
    private String city;
    private String state;
    private String country;
    private String zipCode;

    @DecimalMin(value = "0.0", message = "Minimum salary must not be negative")
    private BigDecimal minSalary;

    @DecimalMin(value = "0.0", message = "Maximum salary must not be negative")
    private BigDecimal maxSalary;

    @NotNull(message = "Job Type is required")
    private JobType jobType;

    @NotNull(message = "Work Mode is required")
    private WorkMode workMode;

    @NotNull(message = "Experience Level is required")
    private ExperienceLevel experienceLevel;

    @Min(value = 1, message = "Openings must be at least 1")
    private Integer openings = 1;

    private LocalDate applicationDeadline;
    private LocalDate expiredAt;

}
