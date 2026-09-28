package com.emmzy.repository;

import com.emmzy.domain.ReminderType;
import com.emmzy.model.InterviewReminder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewReminderRepository extends JpaRepository<InterviewReminder, Long> {

    boolean existsByInterviewIdAndRecipientIdAndReminderType(
            Long interviewId,
            Long recipientId,
            ReminderType reminderType
    );

    void deleteByInterviewId(Long interviewId);
}