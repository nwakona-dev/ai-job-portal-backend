package com.emmzy.repository;

import com.emmzy.model.InterviewReminder;
import com.emmzy.domain.ReminderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterviewReminderRepository
        extends JpaRepository<InterviewReminder, Long> {

    boolean existsByInterviewIdAndRecipientIdAndReminderType(
            Long interviewId,
            Long recipientId,
            ReminderType reminderType
    );


    @Modifying
    @Query("""
            DELETE FROM InterviewReminder r
            WHERE r.interviewId = :interviewId
            """)

    void deleteByInterviewId(
            @Param("interviewId") Long interviewId
    );
}

