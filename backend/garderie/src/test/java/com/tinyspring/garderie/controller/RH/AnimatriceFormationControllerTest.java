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
@DisplayName("Tests AnimatriceFormationController")
class AnimatriceFormationControllerTest {

    @Mock  private IFormationService formationService;
    @InjectMocks private AnimatriceFormationController controller;

    private Formation formation;

    @BeforeEach
    void setUp() {
        formation = new Formation();
        formation.setId(1L); formation.setTitre("Secourisme");
        formation.setType(TypeFormation.SECOURISME);
        formation.setStatut(StatutFormation.OUVERTE);
        formation.setInscriptions(List.of());
    }

    @Test @DisplayName("getFormationsDisponibles — 200")
    void getFormationsDisponibles() {
        when(formationService.getToutesFormations()).thenReturn(List.of(formation));
        ResponseEntity<List<Formation>> r = controller.getFormationsDisponibles();
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).hasSize(1);
    }

    @Test @DisplayName("getFormationById — 200")
    void getFormationById() {
        when(formationService.getFormationById(1L)).thenReturn(formation);
        ResponseEntity<Formation> r = controller.getFormationById(1L);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test @DisplayName("getMonProfil — 200")
    void getMonProfil() {
        when(formationService.getProfilFormations(1L)).thenReturn(Map.of("animatrice", "Sara"));
        ResponseEntity<Map<String, Object>> r = controller.getMonProfil(1L);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test @DisplayName("getMesSuggestions — 200")
    void getMesSuggestions() {
        when(formationService.getSuggestions(1L)).thenReturn(List.of());
        ResponseEntity<List<Map<String, Object>>> r = controller.getMesSuggestions(1L);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test @DisplayName("getMesAlertes — 200")
    void getMesAlertes() {
        when(formationService.getAlertesAnimatrice(1L)).thenReturn(Map.of("totalAlertes", 0));
        ResponseEntity<Map<String, Object>> r = controller.getMesAlertes(1L);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test @DisplayName("sInscrire — 200 si succès")
    void sInscrire_succes() {
        AnimatriceFormation ins = new AnimatriceFormation();
        when(formationService.inscrireAnimatrice(1L, 2L)).thenReturn(ins);
        ResponseEntity<?> r = controller.sInscrire(1L, Map.of("animatriceId", 2L));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test @DisplayName("sInscrire — 400 si erreur")
    void sInscrire_erreur() {
        when(formationService.inscrireAnimatrice(1L, 2L))
                .thenThrow(new RuntimeException("Déjà inscrite"));
        ResponseEntity<?> r = controller.sInscrire(1L, Map.of("animatriceId", 2L));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test @DisplayName("seDesinscrire — 200 si succès")
    void seDesinscrire_succes() {
        doNothing().when(formationService).desinscrireAnimatrice(1L, 2L);
        ResponseEntity<?> r = controller.seDesinscrire(1L, 2L);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test @DisplayName("seDesinscrire — 400 si erreur")
    void seDesinscrire_erreur() {
        doThrow(new RuntimeException("Inscription non trouvée"))
                .when(formationService).desinscrireAnimatrice(1L, 2L);
        ResponseEntity<?> r = controller.seDesinscrire(1L, 2L);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
