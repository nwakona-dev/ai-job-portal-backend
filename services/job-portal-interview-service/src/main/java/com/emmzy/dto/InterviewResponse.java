package com.emmzy.dto;

import com.emmzy.domain.HiringDecision;
import com.emmzy.domain.InterviewStatus;
import com.emmzy.domain.InterviewType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewResponse {

    private Long id;

    private Long applicationId;

    private Long candidateId;

    private String candidateName;

    private Long employerId;

    private Long jobId;

    private LocalDate scheduledDate;

    private LocalTime scheduledTime;

    private Integer durationMinutes;

    private InterviewType interviewType;

    private String meetingLink;

    private String location;

    private String instructions;

    private InterviewStatus status;

    private String feedback;

    private HiringDecision hiringDecision;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}