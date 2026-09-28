package com.emmzy.controller;

import com.emmzy.model.Notification;
import com.emmzy.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications(
            @RequestHeader("X-User-Id") Long recipientId
    ) {

        return ResponseEntity.ok(
                notificationService.getMyNotifications(recipientId)
        );
    }

    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(
            @RequestHeader("X-User-Id") Long recipientId
    ) {

        return ResponseEntity.ok(
                notificationService.getUnreadNotifications(recipientId)
        );
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<String> markAsRead(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long recipientId
    ) {

        notificationService.markAsRead(id, recipientId);

        return ResponseEntity.ok(
                "Notification marked as read"
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteNotification(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long recipientId
    ) {

        notificationService.deleteNotification(
                id,
                recipientId
        );

        return ResponseEntity.ok(
                "Notification deleted successfully"
        );
    }

    @PatchMapping("/read-all")
    public ResponseEntity<String> markAllAsRead(
            @RequestHeader("X-User-Id") Long recipientId
    ) {

        notificationService.markAllAsRead(recipientId);

        return ResponseEntity.ok(
                "All notifications marked as read"
        );
    }
}