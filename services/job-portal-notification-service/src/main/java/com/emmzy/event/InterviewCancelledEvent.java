package com.emmzy.event;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewCancelledEvent {

    private Long interviewId;
    private Long applicationId;
    private Long candidateId;
    private Long employerId;
    private Long jobId;
}