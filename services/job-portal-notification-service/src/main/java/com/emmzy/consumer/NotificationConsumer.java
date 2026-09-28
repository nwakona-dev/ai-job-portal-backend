package com.emmzy.consumer;

import com.emmzy.config.RabbitMQConfig;
import com.emmzy.domain.InterviewStatus;
import com.emmzy.domain.InterviewType;
import com.emmzy.event.InterviewCancelledEvent;
import com.emmzy.event.InterviewCompletedEvent;
import com.emmzy.event.InterviewMeetingLinkAddedEvent;
import com.emmzy.event.InterviewRescheduledEvent;
import com.emmzy.event.InterviewScheduledEvent;
import com.emmzy.model.InterviewSchedule;
import com.emmzy.model.Notification;
import com.emmzy.repository.InterviewScheduleRepository;
import com.emmzy.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import com.emmzy.repository.InterviewReminderRepository;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final InterviewScheduleRepository interviewScheduleRepository;
    private final InterviewReminderRepository interviewReminderRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(
            queues = RabbitMQConfig.INTERVIEW_QUEUE,
            messageConverter = "simpleMessageConverter"
    )
    public void handleInterviewEvent(Message message) throws Exception {

        String routingKey =
                message.getMessageProperties().getReceivedRoutingKey();

        System.out.println(
                "Received routing key: " + routingKey
        );

        if (RabbitMQConfig.INTERVIEW_SCHEDULED_ROUTING_KEY.equals(routingKey)) {

            InterviewScheduledEvent event =
                    objectMapper.readValue(
                            message.getBody(),
                            InterviewScheduledEvent.class
                    );

            InterviewSchedule schedule =
                    InterviewSchedule.builder()
                            .interviewId(event.getInterviewId())
                            .applicationId(event.getApplicationId())
                            .candidateId(event.getCandidateId())
                            .employerId(event.getEmployerId())
                            .jobId(event.getJobId())
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

            interviewScheduleRepository.save(schedule);

            Notification notification = Notification.builder()
                    .recipientId(event.getCandidateId())
                    .type("INTERVIEW_SCHEDULED")
                    .title("Interview Scheduled")
                    .message(
                            "Your interview has been scheduled for "
                                    + event.getScheduledDate()
                                    + " at "
                                    + event.getScheduledTime()
                                    + "."
                    )
                    .build();

            notificationRepository.save(notification);

            System.out.println(
                    "Interview schedule and notification saved for candidate: "
                            + event.getCandidateId()
            );

        } else if (
                RabbitMQConfig.INTERVIEW_RESCHEDULED_ROUTING_KEY.equals(routingKey)
        ) {

            InterviewRescheduledEvent event =
                    objectMapper.readValue(
                            message.getBody(),
                            InterviewRescheduledEvent.class
                    );

            InterviewSchedule schedule =
                    interviewScheduleRepository
                            .findByInterviewId(event.getInterviewId())
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Interview schedule not found: "
                                                    + event.getInterviewId()
                                    )
                            );
            interviewReminderRepository.deleteByInterviewId(
                    event.getInterviewId()
            );

            schedule.setScheduledDate(
                    event.getScheduledDate()
            );

            schedule.setScheduledTime(
                    event.getScheduledTime()
            );

            schedule.setDurationMinutes(
                    event.getDurationMinutes()
            );

            schedule.setInterviewType(
                    InterviewType.valueOf(
                            event.getInterviewType()
                    )
            );

            schedule.setMeetingLink(
                    event.getMeetingLink()
            );

            schedule.setLocation(
                    event.getLocation()
            );

            schedule.setInstructions(
                    event.getInstructions()
            );

            schedule.setStatus(
                    InterviewStatus.RESCHEDULED
            );

            interviewScheduleRepository.save(schedule);

            Notification notification = Notification.builder()
                    .recipientId(event.getCandidateId())
                    .type("INTERVIEW_RESCHEDULED")
                    .title("Interview Rescheduled")
                    .message(
                            "Your interview has been rescheduled to "
                                    + event.getScheduledDate()
                                    + " at "
                                    + event.getScheduledTime()
                                    + "."
                    )
                    .build();

            notificationRepository.save(notification);

            System.out.println(
                    "Interview schedule updated and notification saved for candidate: "
                            + event.getCandidateId()
            );

        } else if (
                RabbitMQConfig.INTERVIEW_CANCELLED_ROUTING_KEY
                        .equals(routingKey)
        ) {

            InterviewCancelledEvent event =
                    objectMapper.readValue(
                            message.getBody(),
                            InterviewCancelledEvent.class
                    );

            InterviewSchedule schedule =
                    interviewScheduleRepository
                            .findByInterviewId(event.getInterviewId())
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Interview schedule not found: "
                                                    + event.getInterviewId()
                                    )
                            );

            schedule.setStatus(
                    InterviewStatus.CANCELLED
            );

            interviewScheduleRepository.save(schedule);

            interviewReminderRepository.deleteByInterviewId(
                    event.getInterviewId()
            );

            Notification notification =
                    Notification.builder()
                            .recipientId(event.getCandidateId())
                            .type("INTERVIEW_CANCELLED")
                            .title("Interview Cancelled")
                            .message(
                                    "Your interview has been cancelled."
                            )
                            .build();

            notificationRepository.save(notification);

            System.out.println(
                    "Interview cancelled. Schedule updated, "
                            + "old reminders removed and notification saved "
                            + "for candidate: "
                            + event.getCandidateId()
            );

        } else if (
                RabbitMQConfig.INTERVIEW_COMPLETED_ROUTING_KEY
                        .equals(routingKey)
        ) {

            InterviewCompletedEvent event =
                    objectMapper.readValue(
                            message.getBody(),
                            InterviewCompletedEvent.class
                    );

            InterviewSchedule schedule =
                    interviewScheduleRepository
                            .findByInterviewId(event.getInterviewId())
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Interview schedule not found: "
                                                    + event.getInterviewId()
                                    )
                            );

            schedule.setStatus(
                    InterviewStatus.COMPLETED
            );

            interviewScheduleRepository.save(schedule);

            interviewReminderRepository.deleteByInterviewId(
                    event.getInterviewId()
            );

            Notification notification =
                    Notification.builder()
                            .recipientId(event.getCandidateId())
                            .type("INTERVIEW_COMPLETED")
                            .title("Interview Completed")
                            .message(
                                    "Your interview has been completed. "
                                            + "The employer will review your interview "
                                            + "and make a hiring decision."
                            )
                            .build();

            notificationRepository.save(notification);

            System.out.println(
                    "Interview completed. Schedule updated, "
                            + "old reminders removed and notification saved "
                            + "for candidate: "
                            + event.getCandidateId()
            );

        } else if (
                RabbitMQConfig.INTERVIEW_MEETING_LINK_ADDED_ROUTING_KEY
                        .equals(routingKey)
        ) {

            InterviewMeetingLinkAddedEvent event =
                    objectMapper.readValue(
                            message.getBody(),
                            InterviewMeetingLinkAddedEvent.class
                    );

            InterviewSchedule schedule =
                    interviewScheduleRepository
                            .findByInterviewId(event.getInterviewId())
                            .orElseThrow(
                                    () -> new RuntimeException(
                                            "Interview schedule not found: "
                                                    + event.getInterviewId()
                                    )
                            );

            schedule.setMeetingLink(
                    event.getMeetingLink()
            );

            interviewScheduleRepository.save(schedule);

            Notification notification = Notification.builder()
                    .recipientId(event.getCandidateId())
                    .type("INTERVIEW_MEETING_LINK_ADDED")
                    .title("Interview Meeting Link Available")
                    .message(
                            "Your meeting link is now available for your interview. "
                                    + "Join here: "
                                    + event.getMeetingLink()
                    )
                    .build();

            notificationRepository.save(notification);

            System.out.println(
                    "Interview meeting link updated and "
                            + "notification saved for candidate: "
                            + event.getCandidateId()
            );
        }
    }
}