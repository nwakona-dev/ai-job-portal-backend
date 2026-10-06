package com.emmzy.service.impl;

import com.emmzy.client.ApplicationServiceClient;
import com.emmzy.domain.HiringDecision;
import com.emmzy.domain.InterviewStatus;
import com.emmzy.domain.InterviewType;
import com.emmzy.dto.AddMeetingLinkRequest;
import com.emmzy.dto.ApplicationResponse;
import com.emmzy.dto.CreateInterviewRequest;
import com.emmzy.dto.HiringDecisionRequest;
import com.emmzy.dto.InterviewFeedbackRequest;
import com.emmzy.dto.InterviewResponse;
import com.emmzy.dto.UpdateInterviewRequest;
import com.emmzy.event.InterviewCancelledEvent;
import com.emmzy.event.InterviewCompletedEvent;
import com.emmzy.event.InterviewHiringDecisionEvent;
import com.emmzy.event.InterviewMeetingLinkAddedEvent;
import com.emmzy.event.InterviewRescheduledEvent;
import com.emmzy.event.InterviewScheduledEvent;
import com.emmzy.mapper.InterviewMapper;
import com.emmzy.model.Interview;
import com.emmzy.publisher.InterviewEventPublisher;
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
    private final InterviewEventPublisher interviewEventPublisher;

    // CREATE INTERVIEW
    @Override
    public InterviewResponse createInterview(
            Long employerId,
            CreateInterviewRequest request
    ) throws Exception {
        ApplicationResponse application = applicationServiceClient.getApplicationById(
                        request.getApplicationId()
                );

        if (application == null) {
            throw new Exception("Application not found");
        }

        // Validate interview type
        if (request.getInterviewType() == InterviewType.ONSITE) {

            if (request.getLocation() == null || request.getLocation().isBlank()) {
                throw new Exception(
                        "Location is required for onsite interviews"
                );
            }
            request.setMeetingLink(null);

        } else if (request.getInterviewType() == InterviewType.ONLINE) {
            request.setLocation(null);
        }

        // Verify employer
        if (!Objects.equals(application.getEmployerId(), employerId)) {
            throw new Exception(
                    "You are not the employer for this application"
            );
        }

        // Prevent interview for withdrawn application
        if ("WITHDRAWN".equals(application.getStatus())) {
            throw new Exception(
                    "A withdrawn application cannot be scheduled for an interview"
            );
        }

        // Validate candidate/job information
        if (application.getCandidate() == null || application.getCandidate().getId() == null) {
            throw new Exception(
                    "Candidate information is missing from the application"
            );
        }

        if (application.getJob() == null || application.getJob().getId() == null) {
            throw new Exception(
                    "Job information is missing from the application"
            );
        }

        // Prevent multiple active interviews
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

        // Create interview entity
        Interview interview = InterviewMapper.toEntity(
                        request,
                        employerId,
                        application.getCandidate().getId(),
                        application.getJob().getId()
                );

        Interview savedInterview = interviewRepository.save(interview);

        // Get information for notification/event
        String candidateName = getCandidateName(application);
        String companyName = getCompanyName(application);
        String jobTitle = getJobTitle(application);

        // Update application status
        applicationServiceClient.updateApplicationStatus(application.getId(), employerId,
                "INTERVIEW_SCHEDULED"
        );

        // Publish interview scheduled event
        InterviewScheduledEvent event = new InterviewScheduledEvent(
                        savedInterview.getId(),
                        savedInterview.getApplicationId(),
                        savedInterview.getCandidateId(),
                        candidateName,
                        savedInterview.getEmployerId(),
                        companyName,
                        savedInterview.getJobId(),
                        jobTitle,
                        savedInterview.getScheduledDate(),
                        savedInterview.getScheduledTime(),
                        savedInterview.getDurationMinutes(),
                        savedInterview.getInterviewType().name(),
                        savedInterview.getMeetingLink(),
                        savedInterview.getLocation(),
                        savedInterview.getInstructions()
                );

        interviewEventPublisher.publishInterviewScheduled(event);

        // Return response
        return enrichWithCandidateName(InterviewMapper.toResponse(savedInterview));
    }

    // GET INTERVIEW BY ID
    @Override
    public InterviewResponse getInterviewById(Long interviewId) throws Exception {

        Interview interview = getInterviewEntity(interviewId);
        return enrichWithCandidateName(InterviewMapper.toResponse(interview));
    }

    // GET CANDIDATE INTERVIEWS
    @Override
    public List<InterviewResponse> getCandidateInterviews(
            Long candidateId
    ) {

        return interviewRepository
                .findByCandidateId(candidateId)
                .stream()
                .map(InterviewMapper::toResponse)
                .map(this::enrichWithCandidateName)
                .toList();
    }

    // GET EMPLOYER INTERVIEWS
    @Override
    public List<InterviewResponse> getEmployerInterviews(
            Long employerId
    ) {

        return interviewRepository
                .findByEmployerId(employerId)
                .stream()
                .map(InterviewMapper::toResponse)
                .map(this::enrichWithCandidateName)
                .toList();
    }

    // UPDATE / RESCHEDULE INTERVIEW
    @Override
    public InterviewResponse updateInterview(
            Long interviewId,
            Long employerId,
            UpdateInterviewRequest request
    ) throws Exception {
        Interview interview = getInterviewEntity(interviewId);

        // Verify employer
        assertEmployer(interview, employerId);

        // Prevent rescheduling completed interview
        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new Exception(
                    "A completed interview cannot be rescheduled"
            );
        }

        // Prevent rescheduling cancelled interview
        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new Exception(
                    "A cancelled interview cannot be rescheduled"
            );
        }

        // Get application information
        ApplicationResponse application = applicationServiceClient.getApplicationById(
                        interview.getApplicationId()
                );

        if (application == null) {
            throw new Exception(
                    "Application not found for this interview"
            );
        }

        interview.setScheduledDate(request.getScheduledDate());
        interview.setScheduledTime(request.getScheduledTime());
        interview.setDurationMinutes(request.getDurationMinutes());

        // Handle interview type
        if (interview.getInterviewType() == InterviewType.ONSITE) {

            if (request.getLocation() == null || request.getLocation().isBlank()) {
                throw new Exception(
                        "Location is required for onsite interviews"
                );
            }

            interview.setLocation(request.getLocation());
            interview.setMeetingLink(null);

        } else if (interview.getInterviewType() == InterviewType.ONLINE) {
            interview.setMeetingLink(request.getMeetingLink());
            interview.setLocation(null);
        }

        interview.setInstructions(request.getInstructions());
        interview.setStatus(InterviewStatus.RESCHEDULED);
        Interview savedInterview = interviewRepository.save(interview);

        // Get information for notification/event
        String candidateName = getCandidateName(application);
        String companyName = getCompanyName(application);
        String jobTitle = getJobTitle(application);

        // Publish rescheduled event
        InterviewRescheduledEvent event = new InterviewRescheduledEvent(
                        savedInterview.getId(),
                        savedInterview.getApplicationId(),
                        savedInterview.getCandidateId(),
                        candidateName,
                        savedInterview.getEmployerId(),
                        companyName,
                        savedInterview.getJobId(),
                        jobTitle,
                        savedInterview.getScheduledDate(),
                        savedInterview.getScheduledTime(),
                        savedInterview.getDurationMinutes(),
                        savedInterview.getInterviewType().name(),
                        savedInterview.getMeetingLink(),
                        savedInterview.getLocation(),
                        savedInterview.getInstructions()
                );

        interviewEventPublisher.publishInterviewRescheduled(event);
        return enrichWithCandidateName(InterviewMapper.toResponse(savedInterview));
    }

    // ADD MEETING LINK
    @Override
    public InterviewResponse addMeetingLink(
            Long interviewId,
            Long employerId,
            AddMeetingLinkRequest request
    ) throws Exception {
        Interview interview = getInterviewEntity(interviewId);

        // Verify employer
        assertEmployer(interview, employerId);

        // Only online interviews can have meeting links
        if (interview.getInterviewType()
                != InterviewType.ONLINE) {

            throw new Exception(
                    "Meeting link can only be added to online interviews"
            );
        }

        // Prevent cancelled interview modification
        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new Exception(
                    "Cannot add a meeting link to a cancelled interview"
            );
        }

        // Prevent completed interview modification
        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new Exception(
                    "Cannot add a meeting link to a completed interview"
            );
        }

        // Save meeting link
        interview.setMeetingLink(request.getMeetingLink());

        Interview savedInterview = interviewRepository.save(interview);

        // Get application information
        ApplicationResponse application = applicationServiceClient.getApplicationById(
                        savedInterview.getApplicationId()
        );

        if (application == null) {
            throw new Exception(
                    "Application not found for this interview"
            );
        }

        String candidateName = getCandidateName(application);
        String companyName = getCompanyName(application);
        String jobTitle = getJobTitle(application);

        // Publish event
        InterviewMeetingLinkAddedEvent event =
                new InterviewMeetingLinkAddedEvent(
                        savedInterview.getId(),
                        savedInterview.getApplicationId(),
                        savedInterview.getCandidateId(),
                        candidateName,
                        savedInterview.getEmployerId(),
                        companyName,
                        savedInterview.getJobId(),
                        jobTitle,
                        savedInterview.getMeetingLink()
                );

        interviewEventPublisher.publishInterviewMeetingLinkAdded(event);
        return enrichWithCandidateName(InterviewMapper.toResponse(savedInterview));
    }

    // CANCEL INTERVIEW
    @Override
    public InterviewResponse cancelInterview(
            Long interviewId,
            Long employerId
    ) throws Exception {
        Interview interview = getInterviewEntity(interviewId);

        // Verify employer
        assertEmployer(interview, employerId);

        // Prevent duplicate cancellation
        if (interview.getStatus() == InterviewStatus.CANCELLED) {

            throw new Exception(
                    "Interview has already been cancelled"
            );
        }

        // Prevent cancelling completed interview
        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new Exception(
                    "A completed interview cannot be cancelled"
            );
        }

        // Get application information BEFORE publishing event
        ApplicationResponse application = applicationServiceClient.getApplicationById(
                    interview.getApplicationId()
                );

        if (application == null) {
            throw new Exception(
                    "Application not found for this interview"
            );
        }

        String candidateName = getCandidateName(application);
        String companyName = getCompanyName(application);
        String jobTitle = getJobTitle(application);

        // Cancel interview
        interview.setStatus(InterviewStatus.CANCELLED);
        Interview savedInterview = interviewRepository.save(interview);

        // Publish event
        InterviewCancelledEvent event = new InterviewCancelledEvent(
                        savedInterview.getId(),
                        savedInterview.getApplicationId(),
                        savedInterview.getCandidateId(),
                        candidateName,
                        savedInterview.getEmployerId(),
                        companyName,
                        savedInterview.getJobId(),
                        jobTitle
                );

        interviewEventPublisher.publishInterviewCancelled(event);
        return enrichWithCandidateName(InterviewMapper.toResponse(savedInterview)
        );
    }

    // ADD FEEDBACK
    @Override
    public InterviewResponse addFeedback(Long interviewId, Long employerId,
            InterviewFeedbackRequest request
    ) throws Exception {
        Interview interview = getInterviewEntity(interviewId);

        // Verify employer
        assertEmployer(interview, employerId);

        // Prevent feedback on cancelled interview
        if (interview.getStatus() == InterviewStatus.CANCELLED) {
            throw new Exception(
                    "Cannot add feedback to a cancelled interview"
            );
        }

        // Prevent duplicate feedback
        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new Exception(
                    "Interview has already been completed"
            );
        }

        // Get application information
        ApplicationResponse application = applicationServiceClient.getApplicationById(
                        interview.getApplicationId());

        if (application == null) {
            throw new Exception(
                    "Application not found for this interview"
            );
        }

        String candidateName = getCandidateName(application);
        String companyName = getCompanyName(application);
        String jobTitle = getJobTitle(application);

        // Save feedback
        interview.setFeedback(request.getFeedback());

        // Mark interview completed
        interview.setStatus(InterviewStatus.COMPLETED);
        Interview savedInterview = interviewRepository.save(interview);

        // Publish event
        InterviewCompletedEvent event = new InterviewCompletedEvent(
                        savedInterview.getId(),
                        savedInterview.getApplicationId(),
                        savedInterview.getCandidateId(),
                        candidateName,
                        savedInterview.getEmployerId(),
                        companyName,
                        savedInterview.getJobId(),
                        jobTitle
                );

        interviewEventPublisher.publishInterviewCompleted(event);
        return enrichWithCandidateName(InterviewMapper.toResponse(savedInterview));
    }

    // MAKE HIRING DECISION
    @Override
    public InterviewResponse makeHiringDecision(Long interviewId,
            Long employerId, HiringDecisionRequest request
    ) throws Exception {
        Interview interview = getInterviewEntity(interviewId);

        // Verify employer
        assertEmployer(interview, employerId);

        // Interview must be completed
        if (interview.getStatus()
                != InterviewStatus.COMPLETED) {

            throw new Exception(
                    "Interview must be completed before making a hiring decision"
            );
        }

        // Prevent duplicate decision
        if (interview.getHiringDecision()
                != HiringDecision.PENDING) {

            throw new Exception(
                    "A hiring decision has already been made for this interview"
            );
        }

        // Validate decision
        if (request.getHiringDecision()
                == HiringDecision.PENDING) {

            throw new Exception(
                    "Hiring decision must be HIRED or REJECTED"
            );
        }

        // Get application information
        ApplicationResponse application =
                applicationServiceClient.getApplicationById(
                        interview.getApplicationId()
                );

        if (application == null) {
            throw new Exception(
                    "Application not found for this interview"
            );
        }

        String candidateName = getCandidateName(application);
        String companyName = getCompanyName(application);
        String jobTitle = getJobTitle(application);

        // Save hiring decision
        interview.setHiringDecision(request.getHiringDecision());

        Interview savedInterview = interviewRepository.save(interview);

        // Update application status
        String applicationStatus =
                request.getHiringDecision()
                        == HiringDecision.HIRED
                        ? "HIRED"
                        : "REJECTED";

        applicationServiceClient.updateApplicationStatus(
                savedInterview.getApplicationId(),
                employerId,
                applicationStatus
        );

        // Publish hiring decision event
        InterviewHiringDecisionEvent event =
                new InterviewHiringDecisionEvent(
                        savedInterview.getId(),
                        savedInterview.getApplicationId(),
                        savedInterview.getCandidateId(),
                        candidateName,
                        savedInterview.getEmployerId(),
                        companyName,
                        savedInterview.getJobId(),
                        jobTitle,
                        savedInterview.getHiringDecision()
                );

        interviewEventPublisher.publishInterviewHiringDecision(event);

        return enrichWithCandidateName(InterviewMapper.toResponse(savedInterview));
    }

    // HELPER METHODS
    private String getCandidateName(ApplicationResponse application) {

        if (application.getCandidate() != null &&
                application.getCandidate().getFullName() != null &&
                !application.getCandidate().getFullName().isBlank()) {

            return application.getCandidate().getFullName();
        }

        return "Interview Candidate";
    }


    private String getCompanyName(ApplicationResponse application) {

        if (application.getCompany() != null &&
                application.getCompany().getName() != null &&
                !application.getCompany().getName().isBlank()) {

            return application.getCompany().getName();
        }

        return "the company";
    }

    private String getJobTitle(ApplicationResponse application) {

        if (application.getJob() != null &&
                application.getJob().getTitle() != null &&
                !application.getJob().getTitle().isBlank()) {

            return application.getJob().getTitle();
        }

        return "the position";
    }

    // ENRICH RESPONSE WITH CANDIDATE NAME
    private InterviewResponse enrichWithCandidateName(
            InterviewResponse response
    ) {

        if (response == null) {
            return null;
        }

        if (response.getCandidateId() == null) {
            response.setCandidateName("Interview Candidate");
            return response;
        }
        response.setCandidateName("Interview Candidate");
        return response;
    }

    // GET INTERVIEW ENTITY
    private Interview getInterviewEntity(Long interviewId) throws Exception {
        return interviewRepository.findById(interviewId)
                .orElseThrow(
                        () -> new Exception(
                                "Interview not found with id: "
                                        + interviewId
                        )
                );
    }

    // VERIFY EMPLOYER
    private void assertEmployer(Interview interview, Long employerId) throws Exception {
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