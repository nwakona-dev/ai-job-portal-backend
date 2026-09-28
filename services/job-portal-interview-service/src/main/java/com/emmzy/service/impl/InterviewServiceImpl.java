package com.emmzy.service.impl;

import com.emmzy.client.ApplicationServiceClient;
import com.emmzy.domain.HiringDecision;
import com.emmzy.domain.InterviewStatus;
import com.emmzy.domain.InterviewType;
import com.emmzy.dto.ApplicationResponse;
import com.emmzy.dto.CreateInterviewRequest;
import com.emmzy.dto.HiringDecisionRequest;
import com.emmzy.dto.InterviewFeedbackRequest;
import com.emmzy.dto.InterviewResponse;
import com.emmzy.dto.UpdateInterviewRequest;
import com.emmzy.event.InterviewCancelledEvent;
import com.emmzy.event.InterviewCompletedEvent;
import com.emmzy.event.InterviewRescheduledEvent;
import com.emmzy.event.InterviewScheduledEvent;
import com.emmzy.mapper.InterviewMapper;
import com.emmzy.model.Interview;
import com.emmzy.publisher.InterviewEventPublisher;
import com.emmzy.repository.InterviewRepository;
import com.emmzy.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.emmzy.dto.AddMeetingLinkRequest;
import com.emmzy.event.InterviewMeetingLinkAddedEvent;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationServiceClient applicationServiceClient;
    private final InterviewEventPublisher interviewEventPublisher;

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

        if (request.getInterviewType() == InterviewType.ONSITE) {

            if (request.getLocation() == null ||
                    request.getLocation().isBlank()) {

                throw new Exception(
                        "Location is required for onsite interviews"
                );
            }

            request.setMeetingLink(null);

        } else if (request.getInterviewType() == InterviewType.ONLINE) {

            request.setLocation(null);
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

        InterviewScheduledEvent event =
                new InterviewScheduledEvent(
                        savedInterview.getId(),
                        savedInterview.getApplicationId(),
                        savedInterview.getCandidateId(),
                        savedInterview.getEmployerId(),
                        savedInterview.getJobId(),
                        savedInterview.getScheduledDate(),
                        savedInterview.getScheduledTime(),
                        savedInterview.getDurationMinutes(),
                        savedInterview.getInterviewType().name(),
                        savedInterview.getMeetingLink(),
                        savedInterview.getLocation(),
                        savedInterview.getInstructions()
                );

        interviewEventPublisher.publishInterviewScheduled(event);

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

        /*
         * Keep the original interview type.
         *
         * ONSITE interviews need a location.
         * ONLINE interviews need a meeting link.
         */
        if (interview.getInterviewType() == InterviewType.ONSITE) {

            if (request.getLocation() == null ||
                    request.getLocation().isBlank()) {

                throw new Exception(
                        "Location is required for onsite interviews"
                );
            }

            interview.setLocation(
                    request.getLocation()
            );

            interview.setMeetingLink(null);

        } else if (interview.getInterviewType() == InterviewType.ONLINE) {

            /*
             * The meeting link can be empty because
             * the employer may add it later.
             */
            interview.setMeetingLink(
                    request.getMeetingLink()
            );

            interview.setLocation(null);
        }

        interview.setInstructions(
                request.getInstructions()
        );

        interview.setStatus(
                InterviewStatus.RESCHEDULED
        );

        Interview savedInterview =
                interviewRepository.save(interview);

        InterviewRescheduledEvent event =
                new InterviewRescheduledEvent(
                        savedInterview.getId(),
                        savedInterview.getApplicationId(),
                        savedInterview.getCandidateId(),
                        savedInterview.getEmployerId(),
                        savedInterview.getJobId(),
                        savedInterview.getScheduledDate(),
                        savedInterview.getScheduledTime(),
                        savedInterview.getDurationMinutes(),
                        savedInterview.getInterviewType().name(),
                        savedInterview.getMeetingLink(),
                        savedInterview.getLocation(),
                        savedInterview.getInstructions()
                );

        interviewEventPublisher.publishInterviewRescheduled(
                event
        );

        return InterviewMapper.toResponse(
                savedInterview
        );
    }

    @Override
    public InterviewResponse addMeetingLink(
            Long interviewId,
            Long employerId,
            AddMeetingLinkRequest request
    ) throws Exception {

        Interview interview =
                getInterviewEntity(interviewId);

        assertEmployer(
                interview,
                employerId
        );

        if (interview.getInterviewType() != InterviewType.ONLINE) {
            throw new Exception(
                    "Meeting link can only be added to online interviews"
            );
        }

        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new Exception(
                    "Cannot add a meeting link to a cancelled interview"
            );
        }

        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new Exception(
                    "Cannot add a meeting link to a completed interview"
            );
        }

        interview.setMeetingLink(
                request.getMeetingLink()
        );

        Interview savedInterview =
                interviewRepository.save(interview);

        InterviewMeetingLinkAddedEvent event =
                new InterviewMeetingLinkAddedEvent(
                        savedInterview.getId(),
                        savedInterview.getApplicationId(),
                        savedInterview.getCandidateId(),
                        savedInterview.getEmployerId(),
                        savedInterview.getJobId(),
                        savedInterview.getMeetingLink()
                );

        interviewEventPublisher.publishInterviewMeetingLinkAdded(
                event
        );

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

        InterviewCancelledEvent event =
                new InterviewCancelledEvent(
                        savedInterview.getId(),
                        savedInterview.getApplicationId(),
                        savedInterview.getCandidateId(),
                        savedInterview.getEmployerId(),
                        savedInterview.getJobId()
                );

        interviewEventPublisher.publishInterviewCancelled(event);

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

        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new Exception(
                    "Interview has already been completed"
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

        InterviewCompletedEvent event =
                new InterviewCompletedEvent(
                        savedInterview.getId(),
                        savedInterview.getApplicationId(),
                        savedInterview.getCandidateId(),
                        savedInterview.getEmployerId(),
                        savedInterview.getJobId()
                );

        interviewEventPublisher.publishInterviewCompleted(event);

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