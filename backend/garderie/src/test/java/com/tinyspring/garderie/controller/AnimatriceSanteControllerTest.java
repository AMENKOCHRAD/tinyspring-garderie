package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.ObservationAiRequestDto;
import com.tinyspring.garderie.dto.ObservationAiResponseDto;
import com.tinyspring.garderie.dto.ObservationCreateDto;
import com.tinyspring.garderie.dto.PriseTraitementCreateDto;
import com.tinyspring.garderie.entity.*;
import com.tinyspring.garderie.service.AnimatriceSanteService;
import com.tinyspring.garderie.service.DuplicateObservationException;
import com.tinyspring.garderie.service.ObservationAiService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnimatriceSanteControllerTest {

    @Mock
    AnimatriceSanteService service;

    @Mock
    ObservationAiService observationAiService;

    @InjectMocks
    AnimatriceSanteController controller;

    @Test
    void enregistrerPrise_retourne400_siServiceThrow() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("a@test.com");
        when(service.enregistrerPrise(eq("a@test.com"), eq(1L), any()))
                .thenThrow(new RuntimeException("bad"));

        var resp = controller.enregistrerPrise(1L, new PriseTraitementCreateDto(), auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void creerObservation_retourne409_siDoublon() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("a@test.com");
        when(service.creerObservation(eq("a@test.com"), eq(2L), any()))
                .thenThrow(new DuplicateObservationException("dup", 10L, 0.9));

        var resp = controller.creerObservation(2L, new ObservationCreateDto(), auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void listerPrises_utilisePeriode_siFromOuToFourni() {
        when(service.listerPrisesParEnfantPeriode(eq(3L), any(), any())).thenReturn(List.of());

        var resp = controller.listerPrises(3L, null, "2026-04-01", "2026-04-02");

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(service).listerPrisesParEnfantPeriode(eq(3L), eq(LocalDate.parse("2026-04-01")), eq(LocalDate.parse("2026-04-02")));
        verify(service, never()).listerPrisesParEnfant(anyLong(), any());
    }

    @Test
    void suggererObservation_delegueAuServiceIA() {
        ObservationAiRequestDto req = new ObservationAiRequestDto();
        ObservationAiResponseDto out = new ObservationAiResponseDto();
        when(observationAiService.generer(req)).thenReturn(out);

        var resp = controller.suggererObservation(req, null);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isSameAs(out);
    }

    @Test
    void listerToutesPrises_mappeDto() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("a@test.com");

        PriseTraitement prise = new PriseTraitement();
        Traitement traitement = new Traitement();
        traitement.setId(7L);
        traitement.setNomTraitement("Sirop");
        ConditionSanitaire cs = new ConditionSanitaire();
        Enfant enfant = new Enfant();
        enfant.setId(9L);
        enfant.setNom("Nom");
        enfant.setPrenom("Prenom");
        cs.setEnfant(enfant);
        traitement.setConditionSanitaire(cs);
        prise.setTraitement(traitement);
        prise.setDatePrise(LocalDate.of(2026, 4, 29));
        prise.setHeurePrevue("09:00");
        User u = new User();
        u.setNom("Ani");
        prise.setDonnePar(u);
        prise.setDonneLe(LocalDateTime.of(2026, 4, 29, 9, 1));

        when(service.listerToutesPrises(eq("a@test.com"), any())).thenReturn(List.of(prise));

        var resp = controller.listerToutesPrises(null, auth);

        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0).getTraitementId()).isEqualTo(7L);
        assertThat(resp.getBody().get(0).getEnfantId()).isEqualTo(9L);
    }
}

