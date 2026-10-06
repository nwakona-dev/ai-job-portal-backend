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
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewReminderServiceImpl
        implements InterviewReminderService {

    private final InterviewScheduleRepository interviewScheduleRepository;
    private final InterviewReminderRepository interviewReminderRepository;
    private final NotificationRepository notificationRepository;

    private static final ZoneId LAGOS_ZONE = ZoneId.of("Africa/Lagos");

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMMM d, yyyy");

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("h:mm a");

//    Checks for interview reminders every 5 minutes.
    @Override
    @Scheduled(fixedRate = 300000)
    public void processReminders() {

        LocalDateTime now = LocalDateTime.now(LAGOS_ZONE);

        List<InterviewSchedule> schedules =
                interviewScheduleRepository.findByStatusInAndScheduledDateGreaterThanEqual(
                                List.of(
                                        InterviewStatus.SCHEDULED,
                                        InterviewStatus.RESCHEDULED
                                ),
                                now.toLocalDate()
                        );

        for (InterviewSchedule schedule : schedules) {
//            Extra safety checks.
            if (schedule.getStatus() == InterviewStatus.CANCELLED) {
                continue;
            }

            if (schedule.getStatus() == InterviewStatus.COMPLETED) {
                continue;
            }

//            Make sure the interview has valid date and time information.
            if (schedule.getScheduledDate() == null
                    || schedule.getScheduledTime() == null) {
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

//            24-hour reminder.
            if (minutesUntilInterview >= 1435
                    && minutesUntilInterview <= 1445) {

                sendReminder(
                        schedule,
                        ReminderType.TWENTY_FOUR_HOURS
                );
            }

//            1-hour reminder.
            if (minutesUntilInterview >= 55
                    && minutesUntilInterview <= 65) {

                sendReminder(
                        schedule,
                        ReminderType.ONE_HOUR
                );
            }
        }
    }

//    Sends the reminder to both the candidate and the employer.
    private void sendReminder(
            InterviewSchedule schedule,
            ReminderType reminderType
    ) {

        sendReminderToRecipient(
                schedule,
                schedule.getCandidateId(),
                reminderType,
                true
        );

        sendReminderToRecipient(
                schedule,
                schedule.getEmployerId(),
                reminderType,
                false
        );
    }

//    Sends a reminder to one recipient.
    private void sendReminderToRecipient(
            InterviewSchedule schedule,
            Long recipientId,
            ReminderType reminderType,
            boolean candidate
    ) {

//        Prevent duplicate reminders.
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

//        Build notification.
        Notification.NotificationBuilder notificationBuilder =
                Notification.builder()
                        .recipientId(recipientId)
                        .type(
                                "INTERVIEW_REMINDER_"
                                        + reminderType.name()
                        )
                        .title(
                                buildTitle(
                                        schedule,
                                        reminderType,
                                        candidate
                                )
                        )
                        .message(
                                buildMessage(
                                        schedule,
                                        reminderType,
                                        candidate
                                )
                        );

//        Add the appropriate action button.
        addAction(
                notificationBuilder,
                schedule
        );

        Notification notification = notificationBuilder.build();
        notificationRepository.save(notification);

//        Record that the reminder has been sent.
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

        interviewReminderRepository.save(reminder);

        System.out.println(
                "Interview "
                        + reminderType
                        + " reminder sent to recipient: "
                        + recipientId
                        + " for interview: "
                        + schedule.getInterviewId()
        );
    }

    /**
     * Builds the notification title.
     */
    private String buildTitle(
            InterviewSchedule schedule,
            ReminderType reminderType,
            boolean candidate
    ) {

        String jobTitle =
                safeValue(
                        schedule.getJobTitle(),
                        "your interview"
                );

        String companyName =
                safeValue(
                        schedule.getCompanyName(),
                        "the company"
                );

        String candidateName =
                safeValue(
                        schedule.getCandidateName(),
                        "the candidate"
                );

        if (reminderType == ReminderType.TWENTY_FOUR_HOURS) {

            if (candidate) {
                return "Interview Tomorrow";
            }

            return "Interview Tomorrow with "
                    + candidateName;
        }

        if (candidate) {
            return "Interview Starting Soon";
        }

        return "Interview Starting Soon with "
                + candidateName;
    }

    /**
     * Builds the actual notification message.
     */
    private String buildMessage(
            InterviewSchedule schedule,
            ReminderType reminderType,
            boolean candidate
    ) {

        String jobTitle =
                safeValue(
                        schedule.getJobTitle(),
                        "the position"
                );

        String companyName =
                safeValue(
                        schedule.getCompanyName(),
                        "the company"
                );

        String candidateName =
                safeValue(
                        schedule.getCandidateName(),
                        "the candidate"
                );

        String formattedDate =
                formatDate(
                        schedule.getScheduledDate()
                );

        String formattedTime =
                formatTime(
                        schedule.getScheduledTime()
                );

        StringBuilder message =
                new StringBuilder();

        /*
         * Candidate message.
         */
        if (candidate) {

            if (reminderType == ReminderType.TWENTY_FOUR_HOURS) {

                message.append(
                                "Your interview for "
                        )
                        .append(jobTitle)
                        .append(" at ")
                        .append(companyName)
                        .append(" is tomorrow at ")
                        .append(formattedTime)
                        .append(".");
            } else {

                message.append(
                                "Your interview for "
                        )
                        .append(jobTitle)
                        .append(" at ")
                        .append(companyName)
                        .append(" starts in about 1 hour at ")
                        .append(formattedTime)
                        .append(".");
            }

        }

        /*
         * Employer message.
         */
        else {

            if (reminderType == ReminderType.TWENTY_FOUR_HOURS) {

                message.append(
                                "Your interview with "
                        )
                        .append(candidateName)
                        .append(" for ")
                        .append(jobTitle)
                        .append(" at ")
                        .append(companyName)
                        .append(" is tomorrow at ")
                        .append(formattedTime)
                        .append(".");
            } else {

                message.append(
                                "Your interview with "
                        )
                        .append(candidateName)
                        .append(" for ")
                        .append(jobTitle)
                        .append(" at ")
                        .append(companyName)
                        .append(" starts in about 1 hour at ")
                        .append(formattedTime)
                        .append(".");
            }
        }

//        Add interview-specific information.
        if (schedule.getInterviewType() != null) {

            switch (schedule.getInterviewType().name()) {
                case "ONLINE":
                    if (schedule.getMeetingLink() != null
                            && !schedule.getMeetingLink().isBlank()) {

                        message.append(
                                " The meeting link is ready."
                        );

                    } else {
                        message.append(
                                " The meeting link has not been provided yet."
                        );
                    }

                    break;

                case "ONSITE":
                    if (schedule.getLocation() != null
                            && !schedule.getLocation().isBlank()) {

                        message.append(
                                        " Location: "
                                )
                                .append(schedule.getLocation())
                                .append(".");
                    } else {

                        message.append(
                                " The interview location has not been provided yet."
                        );
                    }

                    break;

                default:
                    break;
            }
        }

        return message.toString();
    }

    /**
     * Adds the appropriate action button to the notification.
     */
    private void addAction(
            Notification.NotificationBuilder notificationBuilder,
            InterviewSchedule schedule
    ) {

        /*
         * Online interview + meeting link
         *
         * Example:
         *
         * [Join Meeting]
         */
        if (schedule.getInterviewType() != null
                && "ONLINE".equals(
                schedule.getInterviewType().name()
        )
                && schedule.getMeetingLink() != null
                && !schedule.getMeetingLink().isBlank()) {

            notificationBuilder
                    .actionLabel("Join Meeting")
                    .actionUrl(
                            schedule.getMeetingLink()
                    );

            return;
        }

        /*
         * Everything else goes to the interview page.
         */
        notificationBuilder
                .actionLabel("View Interview")
                .actionUrl(
                        "/interviews/"
                                + schedule.getInterviewId()
                );
    }

    /**
     * Formats the interview date.
     *
     * Example:
     * October 3, 2026
     */
    private String formatDate(
            java.time.LocalDate date
    ) {

        if (date == null) {
            return "the scheduled date";
        }

        return date.format(DATE_FORMATTER);
    }

    /**
     * Formats the interview time.
     *
     * Example:
     * 3:00 PM
     */
    private String formatTime(
            java.time.LocalTime time
    ) {

        if (time == null) {
            return "the scheduled time";
        }

        return time.format(TIME_FORMATTER);
    }

    /**
     * Prevents null or blank values from
     * appearing in notifications.
     */
    private String safeValue(
            String value,
            String fallback
    ) {

        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value;
    }
}