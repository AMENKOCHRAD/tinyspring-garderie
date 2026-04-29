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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests AdminAbsenceCongeController")
class AdminAbsenceCongeControllerTest {

    @Mock  private IAbsenceCongeService absenceCongeService;
    @InjectMocks private AdminAbsenceCongeController controller;

    private AbsenceCongeDTO absenceDTO;

    @BeforeEach
    void setUp() {
        absenceDTO = AbsenceCongeDTO.builder()
                .id(1L).animatriceId(1L)
                .animatriceNom("Ben Ali").animatricePrenom("Sara")
                .type(TypeAbsenceConge.CONGE_ANNUEL)
                .dateDebut(LocalDate.now().plusDays(10))
                .dateFin(LocalDate.now().plusDays(15))
                .statut(StatutAbsenceConge.EN_ATTENTE).nbJours(5)
                .build();
    }

    @Test
    @DisplayName("getAllAbsenceConges — doit retourner 200 avec liste")
    void getAllAbsenceConges_doitRetourner200() {
        when(absenceCongeService.getAllAbsenceConges()).thenReturn(List.of(absenceDTO));

        ResponseEntity<List<AbsenceCongeDTO>> response = controller.getAllAbsenceConges();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getAnimatriceNom()).isEqualTo("Ben Ali");
    }

    @Test
    @DisplayName("getById — doit retourner 200 avec le DTO")
    void getById_doitRetourner200() {
        when(absenceCongeService.getAbsenceCongeById(1L)).thenReturn(absenceDTO);

        ResponseEntity<AbsenceCongeDTO> response = controller.getById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getByStatut — doit retourner 200 filtré par statut")
    void getByStatut_doitRetourner200() {
        when(absenceCongeService.getAbsenceCongesByStatut(StatutAbsenceConge.EN_ATTENTE))
                .thenReturn(List.of(absenceDTO));

        ResponseEntity<List<AbsenceCongeDTO>> response = controller.getByStatut(StatutAbsenceConge.EN_ATTENTE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("valider — doit retourner 200 avec statut APPROUVE")
    void valider_doitRetourner200() {
        absenceDTO.setStatut(StatutAbsenceConge.APPROUVE);
        when(absenceCongeService.validerDemande(1L)).thenReturn(absenceDTO);

        ResponseEntity<AbsenceCongeDTO> response = controller.valider(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatut()).isEqualTo(StatutAbsenceConge.APPROUVE);
    }

    @Test
    @DisplayName("refuser — doit retourner 200 avec statut REFUSE")
    void refuser_doitRetourner200() {
        absenceDTO.setStatut(StatutAbsenceConge.REFUSE);
        when(absenceCongeService.refuserDemande(1L)).thenReturn(absenceDTO);

        ResponseEntity<AbsenceCongeDTO> response = controller.refuser(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatut()).isEqualTo(StatutAbsenceConge.REFUSE);
    }

    @Test
    @DisplayName("delete — doit retourner 204")
    void delete_doitRetourner204() {
        doNothing().when(absenceCongeService).deleteAbsenceConge(1L);

        ResponseEntity<Void> response = controller.delete(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(absenceCongeService).deleteAbsenceConge(1L);
    }
}