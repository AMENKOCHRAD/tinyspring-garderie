package com.tinyspring.garderie.controller.RH;

import com.tinyspring.garderie.dto.RH.QuotaCongeDTO;
import com.tinyspring.garderie.entity.RH.AbsenceConge;
import com.tinyspring.garderie.entity.RH.QuotaConge;
import com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge;
import com.tinyspring.garderie.repository.RH.AbsenceCongeRepository;
import com.tinyspring.garderie.repository.RH.QuotaCongeRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests QuotaCongeController")
class QuotaCongeControllerTest {

    @Mock  private QuotaCongeRepository quotaCongeRepository;
    @Mock  private AbsenceCongeRepository absenceCongeRepository;
    @InjectMocks private QuotaCongeController controller;

    @Test @DisplayName("getAllQuotas — 200 avec quota par défaut")
    void getAllQuotas_quotaParDefaut() {
        when(quotaCongeRepository.findByType(any())).thenReturn(Optional.empty());
        when(absenceCongeRepository.findAll()).thenReturn(List.of());

        ResponseEntity<List<QuotaCongeDTO>> r = controller.getAllQuotas();

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).hasSize(4); // 4 types d'absence
    }

    @Test @DisplayName("getAllQuotas — 200 avec quota en BD")
    void getAllQuotas_quotaEnBD() {
        QuotaConge quota = QuotaConge.builder()
                .type(TypeAbsenceConge.CONGE_ANNUEL).nbJoursMax(25)
                .delaiPrevenanceJours(5).effectifMinimum(2).autoApprobation(true).build();

        // ✅ Un seul mock avec any() — évite le UnnecessaryStubbing
        when(quotaCongeRepository.findByType(any())).thenReturn(Optional.of(quota));
        when(absenceCongeRepository.findAll()).thenReturn(List.of());

        ResponseEntity<List<QuotaCongeDTO>> r = controller.getAllQuotas();

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).hasSize(4);
        assertThat(r.getBody().get(0).getNbJoursMax()).isEqualTo(25);
    }

    @Test @DisplayName("getAllQuotas — calcule jours utilisés correctement")
    void getAllQuotas_calculJoursUtilises() {
        AbsenceConge absence = AbsenceConge.builder()
                .type(TypeAbsenceConge.CONGE_ANNUEL)
                .dateDebut(LocalDate.now())
                .dateFin(LocalDate.now().plusDays(4))
                .nbJours(5).build();

        when(quotaCongeRepository.findByType(any())).thenReturn(Optional.empty());
        when(absenceCongeRepository.findAll()).thenReturn(List.of(absence));

        ResponseEntity<List<QuotaCongeDTO>> r = controller.getAllQuotas();

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        QuotaCongeDTO congeAnnuel = r.getBody().stream()
                .filter(q -> q.getType() == TypeAbsenceConge.CONGE_ANNUEL)
                .findFirst().orElseThrow();
        assertThat(congeAnnuel.getJoursUtilisesAnneeEnCours()).isEqualTo(5);
    }

    @Test @DisplayName("updateQuota — 200 met à jour et sauvegarde")
    void updateQuota() {
        QuotaConge existant = QuotaConge.builder()
                .type(TypeAbsenceConge.CONGE_ANNUEL).nbJoursMax(30)
                .delaiPrevenanceJours(7).effectifMinimum(2).autoApprobation(true).build();
        QuotaConge sauvegarde = QuotaConge.builder()
                .type(TypeAbsenceConge.CONGE_ANNUEL).nbJoursMax(25)
                .delaiPrevenanceJours(5).effectifMinimum(1).autoApprobation(false).build();

        when(quotaCongeRepository.findByType(TypeAbsenceConge.CONGE_ANNUEL))
                .thenReturn(Optional.of(existant));
        when(quotaCongeRepository.save(any())).thenReturn(sauvegarde);

        QuotaCongeDTO dto = QuotaCongeDTO.builder()
                .nbJoursMax(25).delaiPrevenanceJours(5)
                .effectifMinimum(1).autoApprobation(false).build();

        ResponseEntity<QuotaCongeDTO> r = controller.updateQuota(TypeAbsenceConge.CONGE_ANNUEL, dto);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().getNbJoursMax()).isEqualTo(25);
        verify(quotaCongeRepository).save(any());
    }

    @Test @DisplayName("updateQuota — crée nouveau quota si inexistant en BD")
    void updateQuota_nouveauQuota() {
        QuotaConge nouveau = QuotaConge.builder()
                .type(TypeAbsenceConge.ABSENCE).nbJoursMax(8)
                .delaiPrevenanceJours(2).effectifMinimum(2).autoApprobation(false).build();

        when(quotaCongeRepository.findByType(TypeAbsenceConge.ABSENCE))
                .thenReturn(Optional.empty());
        when(quotaCongeRepository.save(any())).thenReturn(nouveau);

        QuotaCongeDTO dto = QuotaCongeDTO.builder()
                .nbJoursMax(8).delaiPrevenanceJours(2)
                .effectifMinimum(2).autoApprobation(false).build();

        ResponseEntity<QuotaCongeDTO> r = controller.updateQuota(TypeAbsenceConge.ABSENCE, dto);

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(quotaCongeRepository).save(any());
    }
}