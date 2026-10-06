package com.emmzy.model;

import com.emmzy.domain.InterviewStatus;
import com.emmzy.domain.InterviewType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "interview_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long interviewId;

    @Column(nullable = false)
    private Long applicationId;

    @Column(nullable = false)
    private Long candidateId;
    private String candidateName;

    @Column(nullable = false)
    private Long employerId;
    private String companyName;

    @Column(nullable = false)
    private Long jobId;
    private String jobTitle;

    @Column(nullable = false)
    private LocalDate scheduledDate;

    @Column(nullable = false)
    private LocalTime scheduledTime;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewType interviewType;

    private String meetingLink;

    private String location;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InterviewStatus status = InterviewStatus.SCHEDULED;
}