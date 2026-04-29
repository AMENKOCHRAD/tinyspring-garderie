package com.tinyspring.garderie.controller.RH;

import com.tinyspring.garderie.entity.RH.AnimatriceFormation;
import com.tinyspring.garderie.entity.RH.Formation;
import com.tinyspring.garderie.entity.RH.enums.StatutFormation;
import com.tinyspring.garderie.entity.RH.enums.TypeFormation;
import com.tinyspring.garderie.service.RH.IFormationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests AdminFormationController")
class AdminFormationControllerTest {

    @Mock  private IFormationService formationService;
    @InjectMocks private AdminFormationController controller;

    private Formation formation;

    @BeforeEach
    void setUp() {
        formation = new Formation();
        formation.setId(1L); formation.setTitre("Secourisme");
        formation.setType(TypeFormation.SECOURISME);
        formation.setStatut(StatutFormation.OUVERTE);
        formation.setInscriptions(List.of());
    }

    @Test @DisplayName("getToutesFormations — 200")
    void getToutesFormations() {
        when(formationService.getToutesFormations()).thenReturn(List.of(formation));
        ResponseEntity<List<Formation>> r = controller.getToutesFormations();
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).hasSize(1);
    }

    @Test @DisplayName("getFormationById — 200")
    void getFormationById() {
        when(formationService.getFormationById(1L)).thenReturn(formation);
        ResponseEntity<Formation> r = controller.getFormationById(1L);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().getTitre()).isEqualTo("Secourisme");
    }

    @Test @DisplayName("creerFormation — 200")
    void creerFormation() {
        when(formationService.creerFormation(any())).thenReturn(formation);
        ResponseEntity<Formation> r = controller.creerFormation(formation);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test @DisplayName("modifierFormation — 200")
    void modifierFormation() {
        when(formationService.modifierFormation(eq(1L), any())).thenReturn(formation);
        ResponseEntity<Formation> r = controller.modifierFormation(1L, formation);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(formationService).modifierFormation(eq(1L), any());
    }

    @Test @DisplayName("supprimerFormation — 204")
    void supprimerFormation() {
        doNothing().when(formationService).supprimerFormation(1L);
        ResponseEntity<Void> r = controller.supprimerFormation(1L);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(formationService).supprimerFormation(1L);
    }

    @Test @DisplayName("demarrerFormation — 200")
    void demarrerFormation() {
        formation.setStatut(StatutFormation.EN_COURS);
        when(formationService.demarrerFormation(1L)).thenReturn(formation);
        ResponseEntity<Formation> r = controller.demarrerFormation(1L);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test @DisplayName("terminerFormation — 200")
    void terminerFormation() {
        formation.setStatut(StatutFormation.TERMINEE);
        when(formationService.terminerFormation(1L)).thenReturn(formation);
        ResponseEntity<Formation> r = controller.terminerFormation(1L);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test @DisplayName("annulerFormation — 200")
    void annulerFormation() {
        formation.setStatut(StatutFormation.ANNULEE);
        when(formationService.annulerFormation(eq(1L), any())).thenReturn(formation);
        ResponseEntity<Formation> r = controller.annulerFormation(1L, Map.of("motif", "Test"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test @DisplayName("annulerFormation — motif par défaut si absent")
    void annulerFormation_motifParDefaut() {
        when(formationService.annulerFormation(eq(1L), eq("Annulation administrative")))
                .thenReturn(formation);
        controller.annulerFormation(1L, Map.of());
        verify(formationService).annulerFormation(1L, "Annulation administrative");
    }

    @Test @DisplayName("inscrireAnimatrice — 200")
    void inscrireAnimatrice() {
        AnimatriceFormation ins = new AnimatriceFormation();
        when(formationService.inscrireAnimatrice(1L, 2L)).thenReturn(ins);
        ResponseEntity<AnimatriceFormation> r = controller.inscrireAnimatrice(1L, 2L);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test @DisplayName("desinscrireAnimatrice — 200 avec message")
    void desinscrireAnimatrice() {
        doNothing().when(formationService).desinscrireAnimatrice(1L, 2L);
        ResponseEntity<Map<String, String>> r = controller.desinscrireAnimatrice(1L, 2L);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).containsKey("message");
    }

    @Test @DisplayName("getSuggestions — 200")
    void getSuggestions() {
        when(formationService.getSuggestions(1L)).thenReturn(List.of());
        ResponseEntity<List<Map<String, Object>>> r = controller.getSuggestions(1L);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test @DisplayName("getAlertesGlobales — 200")
    void getAlertesGlobales() {
        when(formationService.getAlertesGlobales()).thenReturn(Map.of("totalAlertes", 0));
        ResponseEntity<Map<String, Object>> r = controller.getAlertesGlobales();
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test @DisplayName("getStats — 200")
    void getStats() {
        when(formationService.getStatsFormations()).thenReturn(Map.of("total", 5L));
        ResponseEntity<Map<String, Object>> r = controller.getStats();
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().get("total")).isEqualTo(5L);
    }
}
