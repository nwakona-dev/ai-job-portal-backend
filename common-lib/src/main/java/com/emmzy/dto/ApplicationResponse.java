package com.emmzy.dto;

import com.emmzy.domain.AiShortListStatus;
import com.emmzy.domain.ApplicationStatus;
import com.emmzy.dto.response.UserResponse;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponse {

    private Long id;
    private UserResponse candidate;
    private Long employerId;

    private JobResponse job;
    private CompanyResponse company;

    private ApplicationStatus status;

    private Long resumeId;
    private String coverLetter;

    private BigDecimal expectedSalary;
    private LocalDate availableFrom;

    private Boolean isStarred;

    private Integer aiScore;

    private AiShortListStatus aiShortListStatus;

    //todo
//    private List<ApplicationNoteResponse> notes;

    private LocalDateTime withDrawnAt;
    private String withdrawnReason;

    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
    
//    private ApplicationScreeningResponse screening;
}
