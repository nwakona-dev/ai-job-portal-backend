package com.emmzy.mapper;

import com.emmzy.dto.CreateInterviewRequest;
import com.emmzy.dto.InterviewResponse;
import com.emmzy.model.Interview;

public class InterviewMapper {

    private InterviewMapper() {
        // Utility class
    }

    public static Interview toEntity(
            CreateInterviewRequest request,
            Long employerId,
            Long candidateId,
            Long jobId
    ) {

        return Interview.builder()
                .applicationId(request.getApplicationId())
                .candidateId(candidateId)
                .employerId(employerId)
                .jobId(jobId)
                .scheduledDate(request.getScheduledDate())
                .scheduledTime(request.getScheduledTime())
                .durationMinutes(request.getDurationMinutes())
                .interviewType(request.getInterviewType())
                .meetingLink(request.getMeetingLink())
                .location(request.getLocation())
                .instructions(request.getInstructions())
                .build();
    }

    public static InterviewResponse toResponse(
            Interview interview
    ) {

        return InterviewResponse.builder()
                .id(interview.getId())
                .applicationId(interview.getApplicationId())
                .candidateId(interview.getCandidateId())
                .employerId(interview.getEmployerId())
                .jobId(interview.getJobId())
                .scheduledDate(interview.getScheduledDate())
                .scheduledTime(interview.getScheduledTime())
                .durationMinutes(interview.getDurationMinutes())
                .interviewType(interview.getInterviewType())
                .meetingLink(interview.getMeetingLink())
                .location(interview.getLocation())
                .instructions(interview.getInstructions())
                .status(interview.getStatus())
                .feedback(interview.getFeedback())
                .hiringDecision(interview.getHiringDecision())
                .createdAt(interview.getCreatedAt())
                .updatedAt(interview.getUpdatedAt())
                .build();
    }
}