package com.tinyspring.garderie.controller.Events;

import com.tinyspring.garderie.dto.Events.NotificationResponse;
import com.tinyspring.garderie.entity.Events.Notification;
import com.tinyspring.garderie.repository.Events.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parent/notifications")
@RequiredArgsConstructor
public class ParentNotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    public List<NotificationResponse> getNotifications(@RequestParam Long parentId) {
        return notificationRepository.findByParentIdOrderByCreatedAtDesc(parentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PutMapping("/{id}/seen")
    public NotificationResponse markAsSeen(@PathVariable Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow();

        notification.setSeen(true);
        Notification saved = notificationRepository.save(notification);

        return toResponse(saved);
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .seen(notification.isSeen())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}

