package com.emmzy.service.impl;

import com.emmzy.client.ApplicationServiceClient;
import com.emmzy.domain.HiringDecision;
import com.emmzy.domain.InterviewStatus;
import com.emmzy.dto.ApplicationResponse;
import com.emmzy.dto.CreateInterviewRequest;
import com.emmzy.dto.HiringDecisionRequest;
import com.emmzy.dto.InterviewFeedbackRequest;
import com.emmzy.dto.InterviewResponse;
import com.emmzy.dto.UpdateInterviewRequest;
import com.emmzy.domain.ApplicationStatus;
import com.emmzy.mapper.InterviewMapper;
import com.emmzy.model.Interview;
import com.emmzy.repository.InterviewRepository;
import com.emmzy.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationServiceClient applicationServiceClient;

    @Override
    public InterviewResponse createInterview(
            Long employerId,
            CreateInterviewRequest request
    ) throws Exception {

        ApplicationResponse application =
                applicationServiceClient.getApplicationById(
                        request.getApplicationId()
                );

        if (application == null) {
            throw new Exception("Application not found");
        }

        if (!Objects.equals(
                application.getEmployerId(),
                employerId
        )) {
            throw new Exception(
                    "You are not the employer for this application"
            );
        }

        if ("WITHDRAWN".equals(application.getStatus())) {
            throw new Exception(
                    "A withdrawn application cannot be scheduled for an interview"
            );
        }

        List<Interview> existingInterviews =
                interviewRepository.findByApplicationId(
                        application.getId()
                );

        boolean activeInterview =
                existingInterviews.stream()
                        .anyMatch(interview ->
                                interview.getStatus()
                                        != InterviewStatus.CANCELLED
                        );

        if (activeInterview) {
            throw new Exception(
                    "This application already has an active interview"
            );
        }

        Interview interview = InterviewMapper.toEntity(
                request,
                employerId,
                application.getCandidate().getId(),
                application.getJob().getId()
        );

        Interview savedInterview =
                interviewRepository.save(interview);

        applicationServiceClient.updateApplicationStatus(
                application.getId(),
                employerId,
                "INTERVIEW_SCHEDULED"
        );

        return InterviewMapper.toResponse(
                savedInterview
        );
    }

    @Override
    public InterviewResponse getInterviewById(
            Long interviewId
    ) throws Exception {

        Interview interview =
                getInterviewEntity(interviewId);

        return InterviewMapper.toResponse(
                interview
        );
    }

    @Override
    public List<InterviewResponse> getCandidateInterviews(
            Long candidateId
    ) {

        return interviewRepository
                .findByCandidateId(candidateId)
                .stream()
                .map(InterviewMapper::toResponse)
                .toList();
    }

    @Override
    public List<InterviewResponse> getEmployerInterviews(
            Long employerId
    ) {

        return interviewRepository
                .findByEmployerId(employerId)
                .stream()
                .map(InterviewMapper::toResponse)
                .toList();
    }

    @Override
    public InterviewResponse updateInterview(
            Long interviewId,
            Long employerId,
            UpdateInterviewRequest request
    ) throws Exception {

        Interview interview =
                getInterviewEntity(interviewId);

        assertEmployer(
                interview,
                employerId
        );

        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new Exception(
                    "A completed interview cannot be rescheduled"
            );
        }

        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new Exception(
                    "A cancelled interview cannot be rescheduled"
            );
        }

        interview.setScheduledDate(
                request.getScheduledDate()
        );

        interview.setScheduledTime(
                request.getScheduledTime()
        );

        interview.setDurationMinutes(
                request.getDurationMinutes()
        );

        interview.setLocation(
                request.getLocation()
        );

        interview.setInstructions(
                request.getInstructions()
        );

        interview.setStatus(
                InterviewStatus.RESCHEDULED
        );

        Interview savedInterview =
                interviewRepository.save(interview);

        return InterviewMapper.toResponse(
                savedInterview
        );
    }

    @Override
    public InterviewResponse cancelInterview(
            Long interviewId,
            Long employerId
    ) throws Exception {

        Interview interview =
                getInterviewEntity(interviewId);

        assertEmployer(
                interview,
                employerId
        );

        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new Exception(
                    "Interview has already been cancelled"
            );
        }

        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new Exception(
                    "A completed interview cannot be cancelled"
            );
        }

        interview.setStatus(
                InterviewStatus.CANCELLED
        );

        Interview savedInterview =
                interviewRepository.save(interview);

        return InterviewMapper.toResponse(
                savedInterview
        );
    }

    @Override
    public InterviewResponse addFeedback(
            Long interviewId,
            Long employerId,
            InterviewFeedbackRequest request
    ) throws Exception {

        Interview interview =
                getInterviewEntity(interviewId);

        assertEmployer(
                interview,
                employerId
        );

        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new Exception(
                    "Cannot add feedback to a cancelled interview"
            );
        }

        interview.setFeedback(
                request.getFeedback()
        );

        interview.setStatus(
                InterviewStatus.COMPLETED
        );

        Interview savedInterview =
                interviewRepository.save(interview);

        return InterviewMapper.toResponse(
                savedInterview
        );
    }

    @Override
    public InterviewResponse makeHiringDecision(
            Long interviewId,
            Long employerId,
            HiringDecisionRequest request
    ) throws Exception {

        Interview interview =
                getInterviewEntity(interviewId);

        assertEmployer(
                interview,
                employerId
        );

        if (interview.getStatus() != InterviewStatus.COMPLETED) {
            throw new Exception(
                    "Interview must be completed before making a hiring decision"
            );
        }

        if (request.getHiringDecision() == HiringDecision.PENDING) {
            throw new Exception(
                    "Hiring decision must be HIRED or REJECTED"
            );
        }

        interview.setHiringDecision(
                request.getHiringDecision()
        );

        Interview savedInterview =
                interviewRepository.save(interview);

        return InterviewMapper.toResponse(
                savedInterview
        );
    }

    private Interview getInterviewEntity(
            Long interviewId
    ) throws Exception {

        return interviewRepository.findById(interviewId)
                .orElseThrow(
                        () -> new Exception(
                                "Interview not found with id: "
                                        + interviewId
                        )
                );
    }

    private void assertEmployer(
            Interview interview,
            Long employerId
    ) throws Exception {

        if (!Objects.equals(
                interview.getEmployerId(),
                employerId
        )) {

            throw new Exception(
                    "You are not the employer for this interview"
            );
        }
    }
}