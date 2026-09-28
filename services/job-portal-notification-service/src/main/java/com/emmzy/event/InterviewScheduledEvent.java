package com.emmzy.event;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewScheduledEvent {

    private Long interviewId;
    private Long applicationId;
    private Long candidateId;
    private Long employerId;
    private Long jobId;

    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private Integer durationMinutes;

    private String interviewType;

    private String meetingLink;
    private String location;
    private String instructions;
}