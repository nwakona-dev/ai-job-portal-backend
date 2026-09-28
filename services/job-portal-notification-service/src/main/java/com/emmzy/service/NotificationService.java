package com.emmzy.service;

import com.emmzy.model.Notification;

import java.util.List;

public interface NotificationService {

    List<Notification> getMyNotifications(Long recipientId);

    List<Notification> getUnreadNotifications(Long recipientId);

    void markAsRead(Long notificationId, Long recipientId);

    void deleteNotification(Long notificationId, Long recipientId);

    void markAllAsRead(Long recipientId);
}