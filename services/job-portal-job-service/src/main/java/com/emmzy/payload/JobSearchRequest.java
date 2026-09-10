package com.emmzy.payload;

import com.emmzy.domain.ExperienceLevel;
import com.emmzy.domain.JobStatus;
import com.emmzy.domain.JobType;
import com.emmzy.domain.WorkMode;
import lombok.*;
import org.hibernate.jdbc.Work;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSearchRequest {

    private String keyword;
    private String categoryId;
    private List<Long> skillIds;
    private List<Long> tagIds;
    private Long companyId;

    private String location;

    private BigDecimal minSalary;
    private BigDecimal maxSalary;

    private JobType jobType;
    private WorkMode workMode;
    private ExperienceLevel experienceLevel;
    private JobStatus status;

    private Integer minOpenings;
    private Integer maxOpenings;
}
