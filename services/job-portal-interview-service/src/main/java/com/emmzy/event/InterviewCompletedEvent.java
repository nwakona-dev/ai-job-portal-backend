package com.emmzy.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewCompletedEvent {

    private Long interviewId;
    private Long applicationId;
    private Long candidateId;
    private Long employerId;
    private Long jobId;
}