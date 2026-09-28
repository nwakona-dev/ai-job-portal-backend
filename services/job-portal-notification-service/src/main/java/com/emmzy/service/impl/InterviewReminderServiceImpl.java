package com.emmzy.service.impl;

import com.emmzy.domain.InterviewStatus;
import com.emmzy.domain.ReminderType;
import com.emmzy.model.InterviewReminder;
import com.emmzy.model.InterviewSchedule;
import com.emmzy.model.Notification;
import com.emmzy.repository.InterviewReminderRepository;
import com.emmzy.repository.InterviewScheduleRepository;
import com.emmzy.repository.NotificationRepository;
import com.emmzy.service.InterviewReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewReminderServiceImpl
        implements InterviewReminderService {

    private final InterviewScheduleRepository interviewScheduleRepository;
    private final InterviewReminderRepository interviewReminderRepository;
    private final NotificationRepository notificationRepository;

    private static final ZoneId LAGOS_ZONE = ZoneId.of("Africa/Lagos");

    @Override
    @Scheduled(fixedRate = 300000)
    public void processReminders() {

        LocalDateTime now =
                LocalDateTime.now(LAGOS_ZONE);

        List<InterviewSchedule> schedules =
                interviewScheduleRepository
                        .findByStatusInAndScheduledDateGreaterThanEqual(
                                List.of(
                                        InterviewStatus.SCHEDULED,
                                        InterviewStatus.RESCHEDULED
                                ),
                                now.toLocalDate()
                        );

        for (InterviewSchedule schedule : schedules) {

            if (schedule.getStatus() == InterviewStatus.CANCELLED) {
                continue;
            }

            if (schedule.getStatus() == InterviewStatus.COMPLETED) {
                continue;
            }

            LocalDateTime interviewDateTime =
                    LocalDateTime.of(
                            schedule.getScheduledDate(),
                            schedule.getScheduledTime()
                    );

            long minutesUntilInterview =
                    Duration.between(
                            now,
                            interviewDateTime
                    ).toMinutes();

            if (minutesUntilInterview >= 1435
                    && minutesUntilInterview <= 1445) {

                sendReminder(
                        schedule,
                        ReminderType.TWENTY_FOUR_HOURS
                );
            }

            if (minutesUntilInterview >= 55
                    && minutesUntilInterview <= 65) {

                sendReminder(
                        schedule,
                        ReminderType.ONE_HOUR
                );
            }
        }
    }

    private void sendReminder(
            InterviewSchedule schedule,
            ReminderType reminderType
    ) {

        sendReminderToRecipient(
                schedule,
                schedule.getCandidateId(),
                reminderType
        );

        sendReminderToRecipient(
                schedule,
                schedule.getEmployerId(),
                reminderType
        );
    }

    private void sendReminderToRecipient(
            InterviewSchedule schedule,
            Long recipientId,
            ReminderType reminderType
    ) {

        boolean alreadySent =
                interviewReminderRepository
                        .existsByInterviewIdAndRecipientIdAndReminderType(
                                schedule.getInterviewId(),
                                recipientId,
                                reminderType
                        );

        if (alreadySent) {
            return;
        }

        Notification notification =
                Notification.builder()
                        .recipientId(recipientId)
                        .type(
                                "INTERVIEW_REMINDER_"
                                        + reminderType.name()
                        )
                        .title(
                                buildTitle(reminderType)
                        )
                        .message(
                                buildMessage(
                                        schedule,
                                        reminderType
                                )
                        )
                        .build();

        notificationRepository.save(
                notification
        );

        InterviewReminder reminder =
                InterviewReminder.builder()
                        .interviewId(
                                schedule.getInterviewId()
                        )
                        .recipientId(recipientId)
                        .reminderType(reminderType)
                        .sentAt(
                                LocalDateTime.now(
                                        LAGOS_ZONE
                                )
                        )
                        .build();

        interviewReminderRepository.save(
                reminder
        );

        System.out.println(
                "Interview "
                        + reminderType
                        + " reminder sent to recipient: "
                        + recipientId
                        + " for interview: "
                        + schedule.getInterviewId()
        );
    }

    private String buildTitle(
            ReminderType reminderType
    ) {

        if (reminderType == ReminderType.TWENTY_FOUR_HOURS) {
            return "Interview Tomorrow";
        }

        return "Interview Starting Soon";
    }

    private String buildMessage(
            InterviewSchedule schedule,
            ReminderType reminderType
    ) {

        String timeMessage;

        if (reminderType == ReminderType.TWENTY_FOUR_HOURS) {
            timeMessage =
                    "Your interview is tomorrow at "
                            + schedule.getScheduledTime()
                            + ". ";
        } else {
            timeMessage =
                    "Your interview starts in about 1 hour at "
                            + schedule.getScheduledTime()
                            + ". ";
        }

        String interviewDetails = "";

        if (schedule.getInterviewType() != null) {

            if (schedule.getInterviewType().name()
                    .equals("ONLINE")) {

                if (schedule.getMeetingLink() != null
                        && !schedule.getMeetingLink().isBlank()) {

                    interviewDetails =
                            "Meeting link: "
                                    + schedule.getMeetingLink();

                } else {

                    interviewDetails =
                            "The meeting link has not been provided yet.";
                }

            } else if (
                    schedule.getInterviewType().name()
                            .equals("ONSITE")
            ) {

                if (schedule.getLocation() != null
                        && !schedule.getLocation().isBlank()) {

                    interviewDetails =
                            "Location: "
                                    + schedule.getLocation();

                } else {

                    interviewDetails =
                            "Interview location has not been provided yet.";
                }
            }
        }

        return timeMessage + interviewDetails;
    }
}