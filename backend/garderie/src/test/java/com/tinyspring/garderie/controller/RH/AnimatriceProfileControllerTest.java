package com.tinyspring.garderie.controller.RH;

import com.tinyspring.garderie.dto.RH.AnimatriceDTO;
import com.tinyspring.garderie.entity.RH.enums.StatutAnimatrice;
import com.tinyspring.garderie.service.RH.IAnimatriceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests AnimatriceProfileController")
class AnimatriceProfileControllerTest {

    @Mock  private IAnimatriceService animatriceService;
    @InjectMocks private AnimatriceProfileController controller;

    private AnimatriceDTO animatriceDTO;

    @BeforeEach
    void setUp() {
        animatriceDTO = AnimatriceDTO.builder()
                .id(1L).nom("Ben Ali").prenom("Sara")
                .email("sara@tinyspring.com")
                .statut(StatutAnimatrice.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("getMonProfil — doit retourner 200")
    void getMonProfil_doitRetourner200() {
        when(animatriceService.getAnimatriceById(1L)).thenReturn(animatriceDTO);

        ResponseEntity<AnimatriceDTO> response = controller.getMonProfil(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getNom()).isEqualTo("Ben Ali");
    }

    @Test
    @DisplayName("updateMonProfil — doit retourner 200")
    void updateMonProfil_doitRetourner200() {
        when(animatriceService.updateMonProfil(eq(1L), any())).thenReturn(animatriceDTO);

        ResponseEntity<AnimatriceDTO> response = controller.updateMonProfil(1L, animatriceDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getByEmail — doit retourner 200")
    void getByEmail_doitRetourner200() {
        when(animatriceService.getAnimatriceByEmail("sara@tinyspring.com"))
                .thenReturn(animatriceDTO);

        ResponseEntity<AnimatriceDTO> response = controller.getByEmail("sara@tinyspring.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getEmail()).isEqualTo("sara@tinyspring.com");
    }

    @Test
    @DisplayName("mustChangePassword — doit retourner true")
    void mustChangePassword_doitRetournerTrue() {
        when(animatriceService.mustChangePassword(1L)).thenReturn(true);

        ResponseEntity<Map<String, Boolean>> response = controller.mustChangePassword(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("mustChangePassword")).isTrue();
    }

    @Test
    @DisplayName("mustChangePassword — doit retourner false")
    void mustChangePassword_doitRetournerFalse() {
        when(animatriceService.mustChangePassword(1L)).thenReturn(false);

        ResponseEntity<Map<String, Boolean>> response = controller.mustChangePassword(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("mustChangePassword")).isFalse();
    }

    @Test
    @DisplayName("changerMotDePasse — doit retourner 200 si succès")
    void changerMotDePasse_doitRetourner200() {
        doNothing().when(animatriceService).changerMotDePasse(eq(1L), any(), any());

        Map<String, String> body = Map.of(
                "ancienMotDePasse", "ancien123",
                "nouveauMotDePasse", "nouveau123"
        );

        ResponseEntity<Map<String, String>> response = controller.changerMotDePasse(1L, body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("message"))
                .isEqualTo("Mot de passe changé avec succès.");
    }

    @Test
    @DisplayName("changerMotDePasse — doit retourner 400 si champs manquants")
    void changerMotDePasse_doitRetourner400SiChampsManquants() {
        Map<String, String> body = Map.of("ancienMotDePasse", "ancien123");

        ResponseEntity<Map<String, String>> response = controller.changerMotDePasse(1L, body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message"))
                .isEqualTo("Les deux champs sont obligatoires.");
    }

    @Test
    @DisplayName("changerMotDePasse — doit retourner 400 si erreur service")
    void changerMotDePasse_doitRetourner400SiErreur() {
        doThrow(new RuntimeException("Ancien mot de passe incorrect."))
                .when(animatriceService).changerMotDePasse(eq(1L), any(), any());

        Map<String, String> body = Map.of(
                "ancienMotDePasse", "mauvais",
                "nouveauMotDePasse", "nouveau123"
        );

        ResponseEntity<Map<String, String>> response = controller.changerMotDePasse(1L, body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message"))
                .isEqualTo("Ancien mot de passe incorrect.");
    }
}