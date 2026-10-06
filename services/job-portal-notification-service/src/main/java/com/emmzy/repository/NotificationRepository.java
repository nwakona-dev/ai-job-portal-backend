package com.emmzy.repository;

import com.emmzy.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification>
    findByRecipientIdOrderByCreatedAtDesc(
            Long recipientId
    );


    List<Notification>
    findByRecipientIdAndReadFalseOrderByCreatedAtDesc(
            Long recipientId
    );


    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.read = true
            WHERE n.recipientId = :recipientId
            """)
    void markAllAsRead(
            @Param("recipientId") Long recipientId
    );
}

