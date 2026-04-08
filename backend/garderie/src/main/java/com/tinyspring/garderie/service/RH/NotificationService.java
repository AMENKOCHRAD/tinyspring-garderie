package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.NotificationDTO;
import com.tinyspring.garderie.entity.RH.Notification;
import com.tinyspring.garderie.repository.RH.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // ✅ Liste des connexions SSE actives
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    // ===== SSE =====

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        return emitter;
    }

    public void sendToAll(NotificationDTO notification) {
        List<SseEmitter> deadEmitters = new ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notification));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        }

        emitters.removeAll(deadEmitters);
    }

    // ===== CRUD =====

    public NotificationDTO creerNotification(String message, String type) {
        Notification notification = Notification.builder()
                .message(message)
                .type(type)
                .read(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        NotificationDTO dto = toDTO(saved);

        // ✅ Envoyer en temps réel à tous les admins connectés
        sendToAll(dto);

        return dto;
    }

    public List<NotificationDTO> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<NotificationDTO> getNonLues() {
        return notificationRepository.findByReadFalseOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public long countNonLues() {
        return notificationRepository.countByReadFalse();
    }

    public void marquerCommeLue(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    public void marquerToutesCommeLues() {
        List<Notification> nonLues = notificationRepository.findByReadFalseOrderByCreatedAtDesc();
        nonLues.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(nonLues);
    }

    public void supprimerNotification(Long id) {
        notificationRepository.deleteById(id);
    }

    // ===== MAPPING =====

    private NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .message(n.getMessage())
                .type(n.getType())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}