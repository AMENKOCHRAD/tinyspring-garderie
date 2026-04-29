package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.NotificationDTO;
import com.tinyspring.garderie.dto.RH.mapper.NotificationMapper;
import com.tinyspring.garderie.entity.RH.Notification;
import com.tinyspring.garderie.repository.RH.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests NotificationServiceImpl")
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationMapper notificationMapper;

    @InjectMocks private NotificationServiceImpl service;

    private Notification notification;
    private NotificationDTO notificationDTO;

    @BeforeEach
    void setUp() {
        notification = Notification.builder()
                .id(1L)
                .message("Test notification")
                .type("ABSENCE")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationDTO = NotificationDTO.builder()
                .id(1L)
                .message("Test notification")
                .type("ABSENCE")
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ===== creerNotification =====
    @Test
    @DisplayName("creerNotification — doit créer et retourner le DTO")
    void creerNotification_doitCreerEtRetournerDTO() {
        when(notificationRepository.save(any())).thenReturn(notification);
        when(notificationMapper.toDTO(any())).thenReturn(notificationDTO);

        NotificationDTO result = service.creerNotification("Test notification", "ABSENCE");

        assertThat(result).isNotNull();
        assertThat(result.getMessage()).isEqualTo("Test notification");
        verify(notificationRepository).save(any());
    }

    // ===== getAllNotifications =====
    @Test
    @DisplayName("getAllNotifications — doit retourner toutes les notifications")
    void getAllNotifications_doitRetournerToutesNotifications() {
        when(notificationRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(notification));
        when(notificationMapper.toDTO(any())).thenReturn(notificationDTO);

        List<NotificationDTO> result = service.getAllNotifications();

        assertThat(result).hasSize(1);
        verify(notificationRepository).findAllByOrderByCreatedAtDesc();
    }

    // ===== getNonLues =====
    @Test
    @DisplayName("getNonLues — doit retourner les notifications non lues")
    void getNonLues_doitRetournerNonLues() {
        when(notificationRepository.findByReadFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(notification));
        when(notificationMapper.toDTO(any())).thenReturn(notificationDTO);

        List<NotificationDTO> result = service.getNonLues();

        assertThat(result).hasSize(1);
    }

    // ===== countNonLues =====
    @Test
    @DisplayName("countNonLues — doit retourner le nombre de notifications non lues")
    void countNonLues_doitRetournerNombre() {
        when(notificationRepository.countByReadFalse()).thenReturn(5L);

        long result = service.countNonLues();

        assertThat(result).isEqualTo(5L);
    }

    // ===== marquerCommeLue =====
    @Test
    @DisplayName("marquerCommeLue — doit marquer la notification comme lue")
    void marquerCommeLue_doitMarquerCommeLue() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenReturn(notification);

        service.marquerCommeLue(1L);

        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("marquerCommeLue — ne doit rien faire si introuvable")
    void marquerCommeLue_doitNeRienFaireSiIntrouvable() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        service.marquerCommeLue(99L);

        verify(notificationRepository, never()).save(any());
    }

    // ===== marquerToutesCommeLues =====
    @Test
    @DisplayName("marquerToutesCommeLues — doit marquer toutes comme lues")
    void marquerToutesCommeLues_doitMarquerToutes() {
        when(notificationRepository.findByReadFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(notification));
        when(notificationRepository.saveAll(any())).thenReturn(List.of(notification));

        service.marquerToutesCommeLues();

        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).saveAll(any());
    }

    // ===== supprimerNotification =====
    @Test
    @DisplayName("supprimerNotification — doit supprimer par ID")
    void supprimerNotification_doitSupprimerParId() {
        doNothing().when(notificationRepository).deleteById(1L);

        service.supprimerNotification(1L);

        verify(notificationRepository).deleteById(1L);
    }
}