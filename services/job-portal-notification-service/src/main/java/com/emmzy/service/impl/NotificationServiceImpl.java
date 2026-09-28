package com.emmzy.service.impl;

import com.emmzy.model.Notification;
import com.emmzy.repository.NotificationRepository;
import com.emmzy.service.NotificationService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public List<Notification> getMyNotifications(Long recipientId) {

        return notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }

    @Override
    public List<Notification> getUnreadNotifications(Long recipientId) {

        return notificationRepository
                .findByRecipientIdAndReadFalseOrderByCreatedAtDesc(recipientId);
    }

    @Override
    public void markAsRead(
            Long notificationId,
            Long recipientId
    ) {

        Notification notification =
                notificationRepository.findById(notificationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification not found"
                                )
                        );

        if (!notification.getRecipientId().equals(recipientId)) {
            throw new RuntimeException(
                    "You are not allowed to access this notification"
            );
        }

        notification.setRead(true);

        notificationRepository.save(notification);
    }

    @Override
    public void deleteNotification(
            Long notificationId,
            Long recipientId
    ) {

        Notification notification = notificationRepository.findById(notificationId)
                        .orElseThrow(() -> new RuntimeException(
                                        "Notification not found"
                                )
                        );

        if (!notification.getRecipientId().equals(recipientId)) {
            throw new RuntimeException(
                    "You are not allowed to delete this notification"
            );
        }

        notificationRepository.delete(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long recipientId) {

        notificationRepository.markAllAsRead(recipientId);
    }
}