package com.tinyspring.garderie.controller.events;

import com.tinyspring.garderie.dto.Events.NotificationResponse;
import com.tinyspring.garderie.service.events.ParentNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parent/notifications")
@RequiredArgsConstructor
public class ParentNotificationController {

    private final ParentNotificationService parentNotificationService;

    @GetMapping
    public List<NotificationResponse> getNotifications(@RequestParam Long parentId) {
        return parentNotificationService.getNotifications(parentId);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> getUnreadCount(@RequestParam Long parentId) {
        return Map.of("count", parentNotificationService.countUnread(parentId));
    }

    @PutMapping("/{id}/seen")
    public NotificationResponse markAsSeen(@PathVariable Long id) {
        return parentNotificationService.markAsSeen(id);
    }

    @PutMapping("/mark-all-read")
    public void markAllRead(@RequestParam Long parentId) {
        parentNotificationService.markAllRead(parentId);
    }
}