package com.emmzy.consumer;

import com.emmzy.config.RabbitMQConfig;
import com.emmzy.domain.InterviewStatus;
import com.emmzy.domain.InterviewType;
import com.emmzy.event.InterviewCancelledEvent;
import com.emmzy.event.InterviewCompletedEvent;
import com.emmzy.event.InterviewHiringDecisionEvent;
import com.emmzy.event.InterviewMeetingLinkAddedEvent;
import com.emmzy.event.InterviewRescheduledEvent;
import com.emmzy.event.InterviewScheduledEvent;
import com.emmzy.model.InterviewSchedule;
import com.emmzy.model.Notification;
import com.emmzy.repository.InterviewReminderRepository;
import com.emmzy.repository.InterviewScheduleRepository;
import com.emmzy.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final InterviewScheduleRepository interviewScheduleRepository;
    private final InterviewReminderRepository interviewReminderRepository;
    private final ObjectMapper objectMapper;

    // MAIN INTERVIEW EVENT LISTENER
    @RabbitListener(
            queues = RabbitMQConfig.INTERVIEW_QUEUE,
            messageConverter = "simpleMessageConverter"
    )
    @Transactional
    public void handleInterviewEvent(
            Message message
    ) throws Exception {

        String routingKey =
                message.getMessageProperties()
                        .getReceivedRoutingKey();


        System.out.println(
                "[NotificationConsumer] Received event: "
                        + routingKey
        );


        try {
            // INTERVIEW SCHEDULED
            if (RabbitMQConfig.INTERVIEW_SCHEDULED_ROUTING_KEY
                    .equals(routingKey)) {

                handleInterviewScheduled(message);

            }

            // INTERVIEW RESCHEDULED
            else if (RabbitMQConfig.INTERVIEW_RESCHEDULED_ROUTING_KEY
                    .equals(routingKey)) {

                handleInterviewRescheduled(message);

            }

            // INTERVIEW CANCELLED
            else if (RabbitMQConfig.INTERVIEW_CANCELLED_ROUTING_KEY
                    .equals(routingKey)) {

                handleInterviewCancelled(message);

            }

            // INTERVIEW COMPLETED
            else if (RabbitMQConfig.INTERVIEW_COMPLETED_ROUTING_KEY
                    .equals(routingKey)) {

                handleInterviewCompleted(message);

            }

            // MEETING LINK ADDED
            else if (RabbitMQConfig.INTERVIEW_MEETING_LINK_ADDED_ROUTING_KEY
                    .equals(routingKey)) {

                handleMeetingLinkAdded(message);

            }

            // HIRING DECISION
            else if (RabbitMQConfig.INTERVIEW_HIRING_DECISION_ROUTING_KEY
                    .equals(routingKey)) {

                handleHiringDecision(message);

            }

            // UNKNOWN EVENT
            else {

                System.out.println(
                        "[NotificationConsumer] Unknown routing key: "
                                + routingKey
                );
            }


        } catch (Exception e) {
            System.err.println(
                    "[NotificationConsumer] Failed to process event: "
                            + routingKey
            );

            System.err.println(
                    "[NotificationConsumer] Error: "
                            + e.getMessage()
            );

            throw e;
        }
    }

    // INTERVIEW SCHEDULED
    private void handleInterviewScheduled(
            Message message
    ) throws Exception {

        InterviewScheduledEvent event =
                objectMapper.readValue(
                        message.getBody(),
                        InterviewScheduledEvent.class
                );


        validateCandidateId(
                event.getCandidateId()
        );

        // Create interview schedule
        InterviewSchedule schedule =
                InterviewSchedule.builder()
                        .interviewId(event.getInterviewId())
                        .applicationId(event.getApplicationId())
                        .candidateId(event.getCandidateId())
                        .employerId(event.getEmployerId())
                        .jobId(event.getJobId())
                        .candidateName(event.getCandidateName())
                        .companyName(event.getCompanyName())
                        .jobTitle(event.getJobTitle())
                        .scheduledDate(event.getScheduledDate())
                        .scheduledTime(event.getScheduledTime())
                        .durationMinutes(event.getDurationMinutes())
                        .interviewType(
                                InterviewType.valueOf(
                                        event.getInterviewType()
                                )
                        )
                        .meetingLink(event.getMeetingLink())
                        .location(event.getLocation())
                        .instructions(event.getInstructions())
                        .status(InterviewStatus.SCHEDULED)
                        .build();


        interviewScheduleRepository.saveAndFlush(
                schedule
        );

        // Candidate notification
        Notification notification =
                Notification.builder()
                        .recipientId(event.getCandidateId())
                        .type("INTERVIEW_SCHEDULED")
                        .title("Interview Scheduled")
                        .message(
                                buildCandidateScheduledMessage(
                                        event
                                )
                        )
                        .actionLabel("View Interview")
                        .actionUrl(
                                buildInterviewUrl(
                                        event.getInterviewId()
                                )
                        )
                        .build();


        notificationRepository.saveAndFlush(
                notification
        );

        // Employer notification
        Notification employerNotification =
                Notification.builder()
                        .recipientId(event.getEmployerId())
                        .type("INTERVIEW_SCHEDULED")
                        .title("Interview Scheduled")
                        .message(
                                buildEmployerScheduledMessage(
                                        event
                                )
                        )
                        .actionLabel("View Interview")
                        .actionUrl(
                                buildInterviewUrl(
                                        event.getInterviewId()
                                )
                        )
                        .build();


        notificationRepository.saveAndFlush(employerNotification);


        System.out.println(
                "[NotificationConsumer] Interview scheduled notification created"
        );
    }

    // INTERVIEW RESCHEDULED
    private void handleInterviewRescheduled(
            Message message
    ) throws Exception {

        InterviewRescheduledEvent event =
                objectMapper.readValue(
                        message.getBody(),
                        InterviewRescheduledEvent.class
                );


        validateCandidateId(
                event.getCandidateId()
        );


        InterviewSchedule schedule =
                interviewScheduleRepository
                        .findByInterviewId(
                                event.getInterviewId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Interview schedule not found: "
                                                + event.getInterviewId()
                                )
                        );

        // Delete old reminders
        interviewReminderRepository.deleteByInterviewId(
                event.getInterviewId()
        );

        // Update schedule
        schedule.setScheduledDate(event.getScheduledDate());
        schedule.setScheduledTime(event.getScheduledTime());
        schedule.setDurationMinutes(event.getDurationMinutes());
        schedule.setInterviewType(
                InterviewType.valueOf(
                        event.getInterviewType()
                )
        );
        schedule.setMeetingLink(event.getMeetingLink());
        schedule.setLocation(event.getLocation());
        schedule.setInstructions(event.getInstructions());
        schedule.setCandidateName(event.getCandidateName());
        schedule.setCompanyName(event.getCompanyName());
        schedule.setJobTitle(event.getJobTitle());
        schedule.setStatus(InterviewStatus.RESCHEDULED);

        interviewScheduleRepository.saveAndFlush(
                schedule
        );

        // Candidate notification
        Notification notification =
                Notification.builder()
                        .recipientId(event.getCandidateId())
                        .type("INTERVIEW_RESCHEDULED")
                        .title("Interview Rescheduled")
                        .message(
                                buildCandidateRescheduledMessage(
                                        event
                                )
                        )
                        .actionLabel("View Interview")
                        .actionUrl(
                                buildInterviewUrl(
                                        event.getInterviewId()
                                )
                        )
                        .build();


        notificationRepository.saveAndFlush(notification);

        // Employer notification
        Notification employerNotification =
                Notification.builder()
                        .recipientId(event.getEmployerId())
                        .type("INTERVIEW_RESCHEDULED")
                        .title("Interview Rescheduled")
                        .message(
                                buildEmployerRescheduledMessage(
                                        event
                                )
                        )
                        .actionLabel("View Interview")
                        .actionUrl(
                                buildInterviewUrl(
                                        event.getInterviewId()
                                )
                        )
                        .build();


        notificationRepository.saveAndFlush(employerNotification);


        System.out.println(
                "[NotificationConsumer] Interview rescheduled notifications created"
        );
    }

    // INTERVIEW CANCELLED
    private void handleInterviewCancelled(
            Message message
    ) throws Exception {

        InterviewCancelledEvent event =
                objectMapper.readValue(
                        message.getBody(),
                        InterviewCancelledEvent.class
                );


        validateCandidateId(
                event.getCandidateId()
        );


        InterviewSchedule schedule =
                interviewScheduleRepository
                        .findByInterviewId(
                                event.getInterviewId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Interview schedule not found: "
                                                + event.getInterviewId()
                                )
                        );


        schedule.setStatus(InterviewStatus.CANCELLED);
        schedule.setCandidateName(event.getCandidateName());
        schedule.setCompanyName(event.getCompanyName());
        schedule.setJobTitle(event.getJobTitle());

        interviewScheduleRepository.saveAndFlush(schedule);


        // Delete reminders
        interviewReminderRepository.deleteByInterviewId(
                event.getInterviewId()
        );

        // Candidate notification
        Notification notification =
                Notification.builder()
                        .recipientId(event.getCandidateId())
                        .type("INTERVIEW_CANCELLED")
                        .title("Interview Cancelled")
                        .message(
                                buildCandidateCancelledMessage(
                                        event
                                )
                        )
                        .build();


        notificationRepository.saveAndFlush(notification);

        System.out.println(
                "[NotificationConsumer] Interview cancellation notification created"
        );
    }

    // INTERVIEW COMPLETED
    private void handleInterviewCompleted(
            Message message
    ) throws Exception {

        InterviewCompletedEvent event =
                objectMapper.readValue(
                        message.getBody(),
                        InterviewCompletedEvent.class
                );


        validateCandidateId(event.getCandidateId());

        InterviewSchedule schedule =
                interviewScheduleRepository
                        .findByInterviewId(
                                event.getInterviewId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Interview schedule not found: "
                                                + event.getInterviewId()
                                )
                        );


        schedule.setStatus(InterviewStatus.COMPLETED);
        schedule.setCandidateName(event.getCandidateName());
        schedule.setCompanyName(event.getCompanyName());
        schedule.setJobTitle(event.getJobTitle());

        interviewScheduleRepository.saveAndFlush(
                schedule
        );

        // Delete reminders
        interviewReminderRepository.deleteByInterviewId(
                event.getInterviewId()
        );

        // Candidate notification
        Notification notification =
                Notification.builder()
                        .recipientId(event.getCandidateId())
                        .type("INTERVIEW_COMPLETED")
                        .title("Interview Completed")
                        .message(
                                buildCandidateCompletedMessage(
                                        event
                                )
                        )
                        .build();


        notificationRepository.saveAndFlush(notification);

        System.out.println(
                "[NotificationConsumer] Interview completed notification created"
        );
    }

    // MEETING LINK ADDED
    private void handleMeetingLinkAdded(
            Message message
    ) throws Exception {

        InterviewMeetingLinkAddedEvent event =
                objectMapper.readValue(
                        message.getBody(),
                        InterviewMeetingLinkAddedEvent.class
                );


        validateCandidateId(
                event.getCandidateId()
        );

        InterviewSchedule schedule =
                interviewScheduleRepository
                        .findByInterviewId(
                                event.getInterviewId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Interview schedule not found: "
                                                + event.getInterviewId()
                                )
                        );


        schedule.setMeetingLink(event.getMeetingLink());
        schedule.setCandidateName(event.getCandidateName());
        schedule.setCompanyName(event.getCompanyName());
        schedule.setJobTitle(event.getJobTitle());

        interviewScheduleRepository.saveAndFlush(schedule);

        // Candidate notification
        Notification notification =
                Notification.builder()
                        .recipientId(event.getCandidateId())
                        .type("INTERVIEW_MEETING_LINK_ADDED")
                        .title("Interview Meeting Link Available")
                        .message(
                                buildMeetingLinkMessage(
                                        event
                                )
                        )
                        .actionLabel("Join Meeting")
                        .actionUrl(
                                event.getMeetingLink()
                        )
                        .build();


        notificationRepository.saveAndFlush(notification);

        System.out.println(
                "[NotificationConsumer] Meeting link notification created"
        );
    }

    // HIRING DECISION
    private void handleHiringDecision(
            Message message
    ) throws Exception {

        InterviewHiringDecisionEvent event =
                objectMapper.readValue(
                        message.getBody(),
                        InterviewHiringDecisionEvent.class
                );


        validateCandidateId(
                event.getCandidateId()
        );


        InterviewSchedule schedule =
                interviewScheduleRepository
                        .findByInterviewId(
                                event.getInterviewId()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Interview schedule not found: "
                                                + event.getInterviewId()
                                )
                        );


        schedule.setCandidateName(event.getCandidateName());
        schedule.setCompanyName(event.getCompanyName());
        schedule.setJobTitle(event.getJobTitle());

        interviewScheduleRepository.saveAndFlush(schedule);

        // Hired
        if (event.getHiringDecision() ==
                com.emmzy.domain.HiringDecision.HIRED) {

            Notification notification =
                    Notification.builder()
                            .recipientId(event.getCandidateId())
                            .type("INTERVIEW_HIRING_DECISION")
                            .title("Congratulations! You've Been Hired")
                            .message(
                                    buildHiredMessage(
                                            event
                                    )
                            )
                            .actionLabel("View Application")
                            .actionUrl(
                                    "/applications/"
                                            + event.getApplicationId()
                            )
                            .build();


            notificationRepository.saveAndFlush(notification);
        }

        // Rejected
        else if (event.getHiringDecision() ==
                com.emmzy.domain.HiringDecision.REJECTED) {

            Notification notification =
                    Notification.builder()
                            .recipientId(event.getCandidateId())
                            .type("INTERVIEW_HIRING_DECISION")
                            .title("Application Update")
                            .message(
                                    buildRejectedMessage(
                                            event
                                    )
                            )
                            .actionLabel("View Application")
                            .actionUrl(
                                    "/applications/"
                                            + event.getApplicationId()
                            )
                            .build();


            notificationRepository.saveAndFlush(notification);
        }

        System.out.println(
                "[NotificationConsumer] Hiring decision notification created"
        );
    }

    // MESSAGE BUILDERS
    private String buildCandidateScheduledMessage(
            InterviewScheduledEvent event
    ) {
        return "Your interview for "
                + safeJobTitle(event.getJobTitle())
                + " at "
                + safeCompanyName(event.getCompanyName())
                + " is scheduled for "
                + formatDate(event.getScheduledDate())
                + " at "
                + formatTime(event.getScheduledTime())
                + ".";
    }


    private String buildEmployerScheduledMessage(
            InterviewScheduledEvent event
    ) {
        return "Your interview with "
                + safeCandidateName(event.getCandidateName())
                + " for "
                + safeJobTitle(event.getJobTitle())
                + " is scheduled for "
                + formatDate(event.getScheduledDate())
                + " at "
                + formatTime(event.getScheduledTime())
                + ".";
    }


    private String buildCandidateRescheduledMessage(
            InterviewRescheduledEvent event
    ) {

        return "Your interview for "
                + safeJobTitle(event.getJobTitle())
                + " at "
                + safeCompanyName(event.getCompanyName())
                + " has been rescheduled to "
                + formatDate(event.getScheduledDate())
                + " at "
                + formatTime(event.getScheduledTime())
                + ".";
    }


    private String buildEmployerRescheduledMessage(
            InterviewRescheduledEvent event
    ) {

        return "The interview with "
                + safeCandidateName(event.getCandidateName())
                + " for "
                + safeJobTitle(event.getJobTitle())
                + " has been rescheduled to "
                + formatDate(event.getScheduledDate())
                + " at "
                + formatTime(event.getScheduledTime())
                + ".";
    }


    private String buildCandidateCancelledMessage(
            InterviewCancelledEvent event
    ) {

        return "Your interview for "
                + safeJobTitle(event.getJobTitle())
                + " at "
                + safeCompanyName(event.getCompanyName())
                + " has been cancelled by the employer.";
    }


    private String buildCandidateCompletedMessage(
            InterviewCompletedEvent event
    ) {

        return "Your interview for "
                + safeJobTitle(event.getJobTitle())
                + " at "
                + safeCompanyName(event.getCompanyName())
                + " has been completed. "
                + "The employer will review your interview and make a hiring decision.";
    }


    private String buildMeetingLinkMessage(
            InterviewMeetingLinkAddedEvent event
    ) {

        return "Your interview for "
                + safeJobTitle(event.getJobTitle())
                + " at "
                + safeCompanyName(event.getCompanyName())
                + " is ready. "
                + "The employer has added a meeting link.";
    }


    private String buildHiredMessage(
            InterviewHiringDecisionEvent event
    ) {

        return "Congratulations! You have been selected for the "
                + safeJobTitle(event.getJobTitle())
                + " position at "
                + safeCompanyName(event.getCompanyName())
                + ".";
    }


    private String buildRejectedMessage(
            InterviewHiringDecisionEvent event
    ) {

        return "Thank you for interviewing for the "
                + safeJobTitle(event.getJobTitle())
                + " position at "
                + safeCompanyName(event.getCompanyName())
                + ". Unfortunately, your application was not selected.";
    }

    // DATE / TIME FORMATTERS
    private String formatDate(
            java.time.LocalDate date
    ) {

        if (date == null) {
            return "the scheduled date";
        }

        return date.format(
                java.time.format.DateTimeFormatter.ofPattern(
                        "MMMM d, yyyy"
                )
        );
    }


    private String formatTime(
            java.time.LocalTime time
    ) {

        if (time == null) {
            return "the scheduled time";
        }

        return time.format(
                java.time.format.DateTimeFormatter.ofPattern(
                        "h:mm a"
                )
        );
    }


    // SAFE VALUE HELPERS
    private String safeCandidateName(
            String candidateName
    ) {

        if (candidateName == null ||
                candidateName.isBlank()) {

            return "the candidate";
        }

        return candidateName;
    }


    private String safeCompanyName(
            String companyName
    ) {

        if (companyName == null ||
                companyName.isBlank()) {

            return "the company";
        }

        return companyName;
    }


    private String safeJobTitle(
            String jobTitle
    ) {

        if (jobTitle == null ||
                jobTitle.isBlank()) {

            return "the position";
        }

        return jobTitle;
    }

    // INTERVIEW URL
    private String buildInterviewUrl(
            Long interviewId
    ) {

        return "/interviews/" + interviewId;
    }

    // VALIDATION
    private void validateCandidateId(
            Long candidateId
    ) {

        if (candidateId == null) {

            throw new IllegalArgumentException(
                    "Candidate ID cannot be null"
            );
        }
    }
}