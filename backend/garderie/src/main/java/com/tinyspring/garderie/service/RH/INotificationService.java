package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.NotificationDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface INotificationService {

    SseEmitter createEmitter();

    void sendToAll(NotificationDTO notification);

    NotificationDTO creerNotification(String message, String type);

    List<NotificationDTO> getAllNotifications();

    List<NotificationDTO> getNonLues();

    long countNonLues();

    void marquerCommeLue(Long id);

    void marquerToutesCommeLues();

    void supprimerNotification(Long id);
}
