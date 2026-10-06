package com.emmzy.service;

import com.emmzy.dto.*;

import java.util.List;

public interface InterviewService {

    InterviewResponse createInterview(Long employerId, CreateInterviewRequest request) throws Exception;

    InterviewResponse getInterviewById(Long interviewId) throws Exception;

    List<InterviewResponse> getCandidateInterviews(Long candidateId);

    List<InterviewResponse> getEmployerInterviews(Long employerId);

    InterviewResponse updateInterview(
            Long interviewId,
            Long employerId,
            UpdateInterviewRequest request
    ) throws Exception;

    InterviewResponse cancelInterview(Long interviewId, Long employerId) throws Exception;

    InterviewResponse addFeedback(
            Long interviewId,
            Long employerId,
            InterviewFeedbackRequest request
    ) throws Exception;

    InterviewResponse makeHiringDecision(
            Long interviewId,
            Long employerId,
            HiringDecisionRequest request
    ) throws Exception;

    InterviewResponse addMeetingLink(
            Long interviewId,
            Long employerId,
            AddMeetingLinkRequest request
    ) throws Exception;
}