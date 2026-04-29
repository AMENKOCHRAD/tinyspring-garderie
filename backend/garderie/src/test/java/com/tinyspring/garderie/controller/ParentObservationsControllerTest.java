package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.ObservationEnfant;
import com.tinyspring.garderie.entity.ObservationType;
import com.tinyspring.garderie.service.ParentObservationsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParentObservationsControllerTest {

    @Mock
    ParentObservationsService service;

    @InjectMocks
    ParentObservationsController controller;

    @Test
    void unreadCount_retourneMapCount() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("p@test.com");
        when(service.compterNonLues("p@test.com")).thenReturn(4L);

        var resp = controller.unreadCount(auth);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).containsEntry("count", 4L);
    }

    @Test
    void lister_mappeObservationEnDto() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("p@test.com");

        ObservationEnfant obs = new ObservationEnfant();
        obs.setType(ObservationType.SANTE);
        obs.setTitre("T");
        obs.setDescription("D");
        obs.setCreeLe(LocalDateTime.of(2026, 4, 29, 10, 0));
        Enfant enfant = new Enfant();
        enfant.setId(2L);
        enfant.setNom("Nom");
        enfant.setPrenom("Prenom");
        obs.setEnfant(enfant);

        when(service.listerObservationsPourParent("p@test.com", 2L)).thenReturn(List.of(obs));

        var resp = controller.lister(2L, auth);

        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0).getType()).isEqualTo("SANTE");
        assertThat(resp.getBody().get(0).getEnfantId()).isEqualTo(2L);
    }
}

