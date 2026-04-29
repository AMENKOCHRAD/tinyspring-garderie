package com.tinyspring.garderie.controller.RH;

import com.tinyspring.garderie.dto.RH.AbsenceCongeDTO;
import com.tinyspring.garderie.entity.RH.enums.StatutAbsenceConge;
import com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge;
import com.tinyspring.garderie.service.RH.IAbsenceCongeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests AnimatriceAbsenceCongeController")
class AnimatriceAbsenceCongeControllerTest {

    @Mock  private IAbsenceCongeService absenceCongeService;
    @InjectMocks private AnimatriceAbsenceCongeController controller;

    private AbsenceCongeDTO absenceDTO;

    @BeforeEach
    void setUp() {
        absenceDTO = AbsenceCongeDTO.builder()
                .id(1L).animatriceId(1L)
                .type(TypeAbsenceConge.CONGE_ANNUEL)
                .dateDebut(LocalDate.now().plusDays(10))
                .dateFin(LocalDate.now().plusDays(15))
                .statut(StatutAbsenceConge.EN_ATTENTE)
                .nbJours(5).build();
    }

    @Test
    @DisplayName("getMesAbsenceConges — doit retourner 200")
    void getMesAbsenceConges_doitRetourner200() {
        when(absenceCongeService.getMesAbsenceConges(1L)).thenReturn(List.of(absenceDTO));

        ResponseEntity<List<AbsenceCongeDTO>> response = controller.getMesAbsenceConges(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getNbJours()).isEqualTo(5);
    }

    @Test
    @DisplayName("soumettreDemande — doit retourner 201")
    void soumettreDemande_doitRetourner201() {
        when(absenceCongeService.soumettreDemandeAbsenceConge(any())).thenReturn(absenceDTO);

        ResponseEntity<AbsenceCongeDTO> response = controller.soumettreDemande(absenceDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isEqualTo(1L);
    }
}