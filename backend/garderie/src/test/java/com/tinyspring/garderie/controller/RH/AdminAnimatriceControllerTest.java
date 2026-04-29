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

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests AdminAnimatriceController")
class AdminAnimatriceControllerTest {

    @Mock  private IAnimatriceService animatriceService;
    @InjectMocks private AdminAnimatriceController controller;

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
    @DisplayName("getAllAnimatrices — doit retourner 200")
    void getAllAnimatrices_doitRetourner200() {
        when(animatriceService.getAllAnimatrices()).thenReturn(List.of(animatriceDTO));

        ResponseEntity<List<AnimatriceDTO>> response = controller.getAllAnimatrices();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getNom()).isEqualTo("Ben Ali");
    }

    @Test
    @DisplayName("getAnimatriceById — doit retourner 200")
    void getAnimatriceById_doitRetourner200() {
        when(animatriceService.getAnimatriceById(1L)).thenReturn(animatriceDTO);

        ResponseEntity<AnimatriceDTO> response = controller.getAnimatriceById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getEmail()).isEqualTo("sara@tinyspring.com");
    }

    @Test
    @DisplayName("getByStatut — doit retourner 200 avec liste filtrée")
    void getByStatut_doitRetourner200() {
        when(animatriceService.getAnimatricesByStatut(StatutAnimatrice.ACTIVE))
                .thenReturn(List.of(animatriceDTO));

        ResponseEntity<List<AnimatriceDTO>> response = controller.getByStatut(StatutAnimatrice.ACTIVE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get(0).getStatut()).isEqualTo(StatutAnimatrice.ACTIVE);
    }

    @Test
    @DisplayName("createAnimatrice — doit retourner 201")
    void createAnimatrice_doitRetourner201() {
        when(animatriceService.createAnimatrice(any())).thenReturn(animatriceDTO);

        ResponseEntity<AnimatriceDTO> response = controller.createAnimatrice(animatriceDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("updateAnimatrice — doit retourner 200")
    void updateAnimatrice_doitRetourner200() {
        when(animatriceService.updateAnimatrice(eq(1L), any())).thenReturn(animatriceDTO);

        ResponseEntity<AnimatriceDTO> response = controller.updateAnimatrice(1L, animatriceDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getNom()).isEqualTo("Ben Ali");
    }

    @Test
    @DisplayName("deleteAnimatrice — doit retourner 204")
    void deleteAnimatrice_doitRetourner204() {
        doNothing().when(animatriceService).deleteAnimatrice(1L);

        ResponseEntity<Void> response = controller.deleteAnimatrice(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(animatriceService).deleteAnimatrice(1L);
    }
}