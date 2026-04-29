package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.RealtimeNotificationDto;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.ObservationEnfant;
import com.tinyspring.garderie.repository.ObservationEnfantRepository;
import com.tinyspring.garderie.websocket.NotificationWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RealtimeNotificationServiceImplTest {

    @Mock
    NotificationWebSocketHandler webSocketHandler;

    @Mock
    ObservationEnfantRepository observationRepository;

    @InjectMocks
    RealtimeNotificationServiceImpl service;

    @Test
    void notifyParentUnreadCountChanged_envoieDtoAvecCompteur() {
        when(observationRepository.countByEnfantParentEmailIgnoreCaseAndLuParentFalse("parent@test.com")).thenReturn(3L);

        service.notifyParentUnreadCountChanged("parent@test.com");

        ArgumentCaptor<RealtimeNotificationDto> captor = ArgumentCaptor.forClass(RealtimeNotificationDto.class);
        verify(webSocketHandler).sendToUser(eq("parent@test.com"), captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(RealtimeNotificationServiceImpl.TYPE_PARENT_UNREAD_COUNT);
        assertThat(captor.getValue().getUnreadObservationsCount()).isEqualTo(3L);
    }

    @Test
    void notifyParentObservationCreated_ignoreSiEmailVide() {
        service.notifyParentObservationCreated(" ", new ObservationEnfant());
        verifyNoInteractions(webSocketHandler);
    }

    @Test
    void notifyParentObservationCreated_envoieAvecIdsEtTitre() {
        ObservationEnfant obs = new ObservationEnfant();
        obs.setTitre("Test");
        obs.setCreeLe(LocalDateTime.of(2026, 4, 29, 8, 0));
        Enfant enfant = new Enfant();
        enfant.setId(99L);
        obs.setEnfant(enfant);

        when(observationRepository.countByEnfantParentEmailIgnoreCaseAndLuParentFalse("parent@test.com")).thenReturn(1L);

        service.notifyParentObservationCreated("parent@test.com", obs);

        ArgumentCaptor<RealtimeNotificationDto> captor = ArgumentCaptor.forClass(RealtimeNotificationDto.class);
        verify(webSocketHandler).sendToUser(eq("parent@test.com"), captor.capture());
        assertThat(captor.getValue().getEnfantId()).isEqualTo(99L);
        assertThat(captor.getValue().getTitre()).isEqualTo("Test");
        assertThat(captor.getValue().getUnreadObservationsCount()).isEqualTo(1L);
    }
}

