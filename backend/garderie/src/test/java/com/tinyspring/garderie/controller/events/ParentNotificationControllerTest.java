package com.tinyspring.garderie.controller.events;

import com.tinyspring.garderie.dto.Events.NotificationResponse;
import com.tinyspring.garderie.service.events.ParentNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParentNotificationControllerTest {

    @Mock
    private ParentNotificationService parentNotificationService;

    @InjectMocks
    private ParentNotificationController controller;

    @Test
    void getNotifications_shouldReturnList() {
        NotificationResponse notif = mock(NotificationResponse.class);

        when(parentNotificationService.getNotifications(1L))
                .thenReturn(List.of(notif));

        List<NotificationResponse> result = controller.getNotifications(1L);

        assertEquals(1, result.size());
        verify(parentNotificationService).getNotifications(1L);
    }

    @Test
    void getUnreadCount_shouldReturnCount() {
        when(parentNotificationService.countUnread(1L)).thenReturn(5L);

        Map<String, Long> result = controller.getUnreadCount(1L);

        assertEquals(5L, result.get("count"));
        verify(parentNotificationService).countUnread(1L);
    }

    @Test
    void markAsSeen_shouldReturnUpdatedNotification() {
        NotificationResponse response = mock( NotificationResponse.class);

        when(parentNotificationService.markAsSeen(1L)).thenReturn(response);

        NotificationResponse result = controller.markAsSeen(1L);

        assertSame(response, result);
        verify(parentNotificationService).markAsSeen(1L);
    }

    @Test
    void markAllRead_shouldCallService() {
        controller.markAllRead(1L);

        verify(parentNotificationService).markAllRead(1L);
    }
}