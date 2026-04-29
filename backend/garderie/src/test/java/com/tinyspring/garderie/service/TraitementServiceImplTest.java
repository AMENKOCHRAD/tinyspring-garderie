package com.tinyspring.garderie.service;

import com.tinyspring.garderie.entity.ConditionSanitaire;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.StatutTraitement;
import com.tinyspring.garderie.entity.Traitement;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.mapper.TraitementMapper;
import com.tinyspring.garderie.repository.ConditionSanitaireRepository;
import com.tinyspring.garderie.repository.TraitementRepository;
import com.tinyspring.garderie.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraitementServiceImplTest {

    @Mock
    TraitementRepository traitementRepository;

    @Mock
    ConditionSanitaireRepository conditionSanitaireRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    OrdonnanceStorageService ordonnanceStorageService;

    @Mock
    TraitementAutoValidationService autoValidationService;

    @Mock
    RealtimeNotificationService realtimeNotificationService;

    @Mock
    TraitementValidationHistoryService validationHistoryService;

    @Mock
    TraitementMapper traitementMapper;

    @InjectMocks
    TraitementServiceImpl service;

    @Test
    void ajouterTraitement_refuseSiDateDebutDansLePasse() {
        Traitement t = new Traitement();
        t.setNomTraitement("Sirop");
        t.setOrdonnance("ord.pdf");
        t.setDateDebut(LocalDate.now().minusDays(1));

        when(conditionSanitaireRepository.findById(1L)).thenReturn(Optional.of(new ConditionSanitaire()));

        assertThatThrownBy(() -> service.ajouterTraitement(1L, t))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("passe");

        verify(traitementRepository, never()).save(any());
        verifyNoInteractions(validationHistoryService);
    }

    @Test
    void ajouterTraitement_accepte_parML_metVALIDE_etNotifieParent_etHistorise() {
        Traitement t = new Traitement();
        t.setNomTraitement("Sirop");
        t.setOrdonnance("ord.pdf");
        t.setDateDebut(LocalDate.now().plusDays(1));
        t.setDateFin(LocalDate.now().plusDays(3));

        ConditionSanitaire condition = conditionAvecParentEmail("parent@test.com");
        when(conditionSanitaireRepository.findById(2L)).thenReturn(Optional.of(condition));

        TraitementAutoValidationService.Result res = new TraitementAutoValidationService.Result();
        res.decision = TraitementAutoValidationService.Decision.ACCEPTE;
        res.note = "OK";
        when(autoValidationService.evaluer(any(Traitement.class))).thenReturn(res);

        when(traitementRepository.save(any(Traitement.class))).thenAnswer(inv -> inv.getArgument(0));

        Traitement saved = service.ajouterTraitement(2L, t);

        assertThat(saved.getStatut()).isEqualTo(StatutTraitement.VALIDE);
        assertThat(saved.getAutoValidationNote()).isEqualTo("OK");

        verify(validationHistoryService).recordSystemDecision(saved, res);
        verify(realtimeNotificationService).notifyParentTraitementValidation(
                eq("parent@test.com"),
                anyLong(),
                any(),
                eq("Sirop"),
                eq("VALIDE"),
                eq("OK")
        );
    }

    @Test
    void ajouterTraitement_aVerifier_gardeEnAttente_etNeNotifiePasParent() {
        Traitement t = new Traitement();
        t.setNomTraitement("Sirop");
        t.setOrdonnance("ord.pdf");
        t.setDateDebut(LocalDate.now().plusDays(1));

        ConditionSanitaire condition = conditionAvecParentEmail("parent@test.com");
        when(conditionSanitaireRepository.findById(3L)).thenReturn(Optional.of(condition));

        TraitementAutoValidationService.Result res = new TraitementAutoValidationService.Result();
        res.decision = TraitementAutoValidationService.Decision.A_VERIFIER;
        res.note = "A verifier";
        when(autoValidationService.evaluer(any(Traitement.class))).thenReturn(res);

        when(traitementRepository.save(any(Traitement.class))).thenAnswer(inv -> inv.getArgument(0));

        Traitement saved = service.ajouterTraitement(3L, t);

        assertThat(saved.getStatut()).isEqualTo(StatutTraitement.EN_ATTENTE_VALIDATION);
        verify(validationHistoryService).recordSystemDecision(saved, res);
        verifyNoInteractions(realtimeNotificationService);
    }

    private static ConditionSanitaire conditionAvecParentEmail(String email) {
        User parent = new User();
        parent.setEmail(email);
        Enfant enfant = new Enfant();
        enfant.setId(11L);
        enfant.setParent(parent);
        ConditionSanitaire condition = new ConditionSanitaire();
        condition.setEnfant(enfant);
        return condition;
    }
}

