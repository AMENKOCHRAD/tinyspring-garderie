package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.AbsenceCongeDTO;
import com.tinyspring.garderie.dto.RH.NotificationDTO;
import com.tinyspring.garderie.dto.RH.ResultatEvaluationDTO;
import com.tinyspring.garderie.dto.RH.mapper.AbsenceCongeMapper;
import com.tinyspring.garderie.entity.RH.AbsenceConge;
import com.tinyspring.garderie.entity.RH.Animatrice;
import com.tinyspring.garderie.entity.RH.enums.StatutAbsenceConge;
import com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge;
import com.tinyspring.garderie.repository.RH.AbsenceCongeRepository;
import com.tinyspring.garderie.repository.RH.AnimatriceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests AbsenceCongeServiceImpl")
class AbsenceCongeServiceImplTest {

    @Mock private AbsenceCongeRepository absenceCongeRepository;
    @Mock private AnimatriceRepository animatriceRepository;
    @Mock private IEmailService emailService;
    @Mock private INotificationService notificationService;
    @Mock private IMoteurReglesService moteurReglesService;
    @Mock private AbsenceCongeMapper absenceCongeMapper;

    @InjectMocks private AbsenceCongeServiceImpl service;

    private Animatrice animatrice;
    private AbsenceConge absence;
    private AbsenceCongeDTO absenceDTO;

    @BeforeEach
    void setUp() {
        animatrice = new Animatrice();
        animatrice.setId(1L);
        animatrice.setNom("Ben Ali");
        animatrice.setPrenom("Sara");
        animatrice.setEmail("sara@tinyspring.com");

        absence = AbsenceConge.builder()
                .animatrice(animatrice)
                .type(TypeAbsenceConge.CONGE_ANNUEL)
                .dateDebut(LocalDate.now().plusDays(10))
                .dateFin(LocalDate.now().plusDays(15))
                .statut(StatutAbsenceConge.EN_ATTENTE)
                .nbJours(5)
                .decisionAutomatique(false)
                .build();
        absence.setId(1L);

        absenceDTO = new AbsenceCongeDTO();
        absenceDTO.setAnimatriceId(1L);
        absenceDTO.setType(TypeAbsenceConge.CONGE_ANNUEL);
        absenceDTO.setDateDebut(LocalDate.now().plusDays(10));
        absenceDTO.setDateFin(LocalDate.now().plusDays(15));
    }

    // ===== getAllAbsenceConges =====
    @Test
    @DisplayName("getAllAbsenceConges — doit retourner toutes les demandes")
    void getAllAbsenceConges_doitRetournerToutesLesDemandes() {
        when(absenceCongeRepository.findAll()).thenReturn(List.of(absence));
        when(absenceCongeMapper.toDTO(any())).thenReturn(absenceDTO);

        List<AbsenceCongeDTO> result = service.getAllAbsenceConges();

        assertThat(result).hasSize(1);
        verify(absenceCongeRepository).findAll();
    }

    // ===== getAbsenceCongeById =====
    @Test
    @DisplayName("getAbsenceCongeById — doit retourner le DTO si trouvé")
    void getAbsenceCongeById_doitRetournerDTO() {
        when(absenceCongeRepository.findById(1L)).thenReturn(Optional.of(absence));
        when(absenceCongeMapper.toDTO(any())).thenReturn(absenceDTO);

        AbsenceCongeDTO result = service.getAbsenceCongeById(1L);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getAbsenceCongeById — doit lever exception si introuvable")
    void getAbsenceCongeById_doitLeverException() {
        when(absenceCongeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAbsenceCongeById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ===== getAbsenceCongesByStatut =====
    @Test
    @DisplayName("getAbsenceCongesByStatut — doit filtrer par statut")
    void getAbsenceCongesByStatut_doitFiltrerParStatut() {
        when(absenceCongeRepository.findByStatut(StatutAbsenceConge.EN_ATTENTE))
                .thenReturn(List.of(absence));
        when(absenceCongeMapper.toDTO(any())).thenReturn(absenceDTO);

        List<AbsenceCongeDTO> result = service.getAbsenceCongesByStatut(StatutAbsenceConge.EN_ATTENTE);

        assertThat(result).hasSize(1);
    }

    // ===== validerDemande =====
    @Test
    @DisplayName("validerDemande — doit changer le statut en APPROUVE")
    void validerDemande_doitChangerStatutEnApprouve() {
        when(absenceCongeRepository.findById(1L)).thenReturn(Optional.of(absence));
        when(absenceCongeRepository.save(any())).thenReturn(absence);
        when(absenceCongeMapper.toDTO(any())).thenReturn(absenceDTO);
        doNothing().when(emailService).envoyerDecisionAbsence(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any());

        service.validerDemande(1L);

        assertThat(absence.getStatut()).isEqualTo(StatutAbsenceConge.APPROUVE);
        assertThat(absence.getDecisionAutomatique()).isFalse();
        verify(emailService).envoyerDecisionAbsence(
                any(), any(), any(), any(), any(), any(), eq(true), any());
    }

    @Test
    @DisplayName("validerDemande — doit lever exception si introuvable")
    void validerDemande_doitLeverException() {
        when(absenceCongeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validerDemande(99L))
                .isInstanceOf(EntityNotFoundException.class).hasMessageContaining("99");
    }

    // ===== refuserDemande =====
    @Test
    @DisplayName("refuserDemande — doit changer le statut en REFUSE")
    void refuserDemande_doitChangerStatutEnRefuse() {
        when(absenceCongeRepository.findById(1L)).thenReturn(Optional.of(absence));
        when(absenceCongeRepository.save(any())).thenReturn(absence);
        when(absenceCongeMapper.toDTO(any())).thenReturn(absenceDTO);
        doNothing().when(emailService).envoyerDecisionAbsence(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any());

        service.refuserDemande(1L);

        assertThat(absence.getStatut()).isEqualTo(StatutAbsenceConge.REFUSE);
        verify(emailService).envoyerDecisionAbsence(
                any(), any(), any(), any(), any(), any(), eq(false), any());
    }

    @Test
    @DisplayName("refuserDemande — doit lever exception si introuvable")
    void refuserDemande_doitLeverException() {
        when(absenceCongeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refuserDemande(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ===== deleteAbsenceConge =====
    @Test
    @DisplayName("deleteAbsenceConge — doit supprimer si existant")
    void deleteAbsenceConge_doitSupprimer() {
        when(absenceCongeRepository.existsById(1L)).thenReturn(true);
        doNothing().when(absenceCongeRepository).deleteById(1L);

        service.deleteAbsenceConge(1L);

        verify(absenceCongeRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteAbsenceConge — doit lever exception si inexistant")
    void deleteAbsenceConge_doitLeverException() {
        when(absenceCongeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteAbsenceConge(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ===== soumettreDemandeAbsenceConge =====
    @Test
    @DisplayName("soumettreDemande — doit lever exception si dates invalides")
    void soumettreDemande_doitLeverExceptionSiDatesInvalides() {
        AbsenceCongeDTO dto = new AbsenceCongeDTO();
        dto.setDateDebut(LocalDate.now().plusDays(10));
        dto.setDateFin(LocalDate.now().plusDays(5));

        assertThatThrownBy(() -> service.soumettreDemandeAbsenceConge(dto))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("date de fin");
    }

    @Test
    @DisplayName("soumettreDemande — doit auto-approuver")
    void soumettreDemande_doitAutoApprouver() {
        when(animatriceRepository.findById(1L)).thenReturn(Optional.of(animatrice));
        when(absenceCongeRepository.save(any())).thenReturn(absence);
        when(absenceCongeRepository.findById(any())).thenReturn(Optional.of(absence));
        when(moteurReglesService.evaluer(any())).thenReturn(
                ResultatEvaluationDTO.builder().decision("AUTO_APPROUVE")
                        .explication("OK").regleDeclenchee("TOUTES_REGLES_OK").build());
        when(absenceCongeMapper.toDTO(any())).thenReturn(absenceDTO);
        doNothing().when(emailService).envoyerDecisionAbsence(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any());
        when(notificationService.creerNotification(any(), any())).thenReturn(new NotificationDTO());

        service.soumettreDemandeAbsenceConge(absenceDTO);

        assertThat(absence.getStatut()).isEqualTo(StatutAbsenceConge.APPROUVE);
        verify(emailService).envoyerDecisionAbsence(
                any(), any(), any(), any(), any(), any(), eq(true), any());
    }

    @Test
    @DisplayName("soumettreDemande — doit auto-refuser")
    void soumettreDemande_doitAutoRefuser() {
        when(animatriceRepository.findById(1L)).thenReturn(Optional.of(animatrice));
        when(absenceCongeRepository.save(any())).thenReturn(absence);
        when(absenceCongeRepository.findById(any())).thenReturn(Optional.of(absence));
        when(moteurReglesService.evaluer(any())).thenReturn(
                ResultatEvaluationDTO.builder().decision("AUTO_REFUSE")
                        .explication("KO").regleDeclenchee("QUOTA_DEPASSE").build());
        when(absenceCongeMapper.toDTO(any())).thenReturn(absenceDTO);
        doNothing().when(emailService).envoyerDecisionAbsence(
                any(), any(), any(), any(), any(), any(), anyBoolean(), any());
        when(notificationService.creerNotification(any(), any())).thenReturn(new NotificationDTO());

        service.soumettreDemandeAbsenceConge(absenceDTO);

        assertThat(absence.getStatut()).isEqualTo(StatutAbsenceConge.REFUSE);
        verify(emailService).envoyerDecisionAbsence(
                any(), any(), any(), any(), any(), any(), eq(false), any());
    }

    @Test
    @DisplayName("soumettreDemande — doit transmettre à l'admin si TRANSMIS_ADMIN")
    void soumettreDemande_doitTransmettreAdmin() {
        when(animatriceRepository.findById(1L)).thenReturn(Optional.of(animatrice));
        when(absenceCongeRepository.save(any())).thenReturn(absence);
        when(absenceCongeRepository.findById(any())).thenReturn(Optional.of(absence));
        when(moteurReglesService.evaluer(any())).thenReturn(
                ResultatEvaluationDTO.builder().decision("TRANSMIS_ADMIN")
                        .explication("Manuel").regleDeclenchee("TOUTES_REGLES_OK").build());
        when(absenceCongeMapper.toDTO(any())).thenReturn(absenceDTO);
        when(notificationService.creerNotification(any(), any())).thenReturn(new NotificationDTO());

        service.soumettreDemandeAbsenceConge(absenceDTO);

        verify(notificationService).creerNotification(any(), any());
    }

    // ===== getMesAbsenceConges =====
    @Test
    @DisplayName("getMesAbsenceConges — doit retourner la liste")
    void getMesAbsenceConges_doitRetournerListe() {
        when(absenceCongeRepository.findByAnimatriceId(1L)).thenReturn(List.of(absence));
        when(absenceCongeMapper.toDTO(any())).thenReturn(absenceDTO);

        List<AbsenceCongeDTO> result = service.getMesAbsenceConges(1L);

        assertThat(result).hasSize(1);
    }
}