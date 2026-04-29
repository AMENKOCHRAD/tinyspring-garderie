package com.tinyspring.garderie.controller.RH;

import com.tinyspring.garderie.dto.RH.NotificationDTO;
import com.tinyspring.garderie.service.RH.INotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests NotificationController")
class NotificationControllerTest {

    @Mock  private INotificationService notificationService;
    @InjectMocks private NotificationController controller;

    private NotificationDTO notificationDTO;

    @BeforeEach
    void setUp() {
        notificationDTO = NotificationDTO.builder()
                .id(1L).message("Test").type("ABSENCE")
                .read(false).createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("getAll — doit retourner 200 avec liste")
    void getAll_doitRetourner200() {
        when(notificationService.getAllNotifications()).thenReturn(List.of(notificationDTO));

        ResponseEntity<List<NotificationDTO>> response = controller.getAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getType()).isEqualTo("ABSENCE");
    }

    @Test
    @DisplayName("getNonLues — doit retourner 200 avec non lues")
    void getNonLues_doitRetourner200() {
        when(notificationService.getNonLues()).thenReturn(List.of(notificationDTO));

        ResponseEntity<List<NotificationDTO>> response = controller.getNonLues();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get(0).isRead()).isFalse();
    }

    @Test
    @DisplayName("count — doit retourner le nombre de non lues")
    void count_doitRetournerNombre() {
        when(notificationService.countNonLues()).thenReturn(5L);

        ResponseEntity<Map<String, Long>> response = controller.count();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("count")).isEqualTo(5L);
    }

    @Test
    @DisplayName("marquerLue — doit appeler le service et retourner 200")
    void marquerLue_doitRetourner200() {
        doNothing().when(notificationService).marquerCommeLue(1L);

        ResponseEntity<Void> response = controller.marquerLue(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationService).marquerCommeLue(1L);
    }

    @Test
    @DisplayName("marquerToutLu — doit appeler le service et retourner 200")
    void marquerToutLu_doitRetourner200() {
        doNothing().when(notificationService).marquerToutesCommeLues();

        ResponseEntity<Void> response = controller.marquerToutLu();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificationService).marquerToutesCommeLues();
    }

    @Test
    @DisplayName("supprimer — doit retourner 204")
    void supprimer_doitRetourner204() {
        doNothing().when(notificationService).supprimerNotification(1L);

        ResponseEntity<Void> response = controller.supprimer(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(notificationService).supprimerNotification(1L);
    }
}