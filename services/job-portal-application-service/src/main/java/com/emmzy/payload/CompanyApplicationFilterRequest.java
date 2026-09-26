package com.emmzy.payload;

import com.emmzy.domain.AiShortListStatus;
import com.emmzy.domain.ApplicationStatus;
import lombok.Data;

@Data
public class CompanyApplicationFilterRequest {

    private Long jobId;
    private ApplicationStatus status;
    private Boolean isStarred;
    private AiShortListStatus aiShortListStatus;
    private Integer minAiScore;
    private String sortBy;
}
