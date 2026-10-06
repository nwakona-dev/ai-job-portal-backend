package com.emmzy.event;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewCompletedEvent {

    private Long interviewId;

    private Long applicationId;

    private Long candidateId;
    private String candidateName;

    private Long employerId;
    private String companyName;

    private Long jobId;
    private String jobTitle;
}