package com.tinyspring.garderie.service;

import com.tinyspring.garderie.entity.ConditionSanitaire;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.TypeConditionSanitaire;
import com.tinyspring.garderie.repository.ConditionSanitaireRepository;
import com.tinyspring.garderie.repository.EnfantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConditionSanitaireServiceImplTest {

    @Mock
    ConditionSanitaireRepository conditionRepo;

    @Mock
    EnfantRepository enfantRepo;

    @InjectMocks
    ConditionSanitaireServiceImpl service;

    @Test
    void addCondition_refuseDateDebutDansLePasse() {
        ConditionSanitaire condition = new ConditionSanitaire();
        condition.setType(TypeConditionSanitaire.MALADIE_CHRONIQUE);
        condition.setNomCondition("Asthme");
        condition.setDateDebut(LocalDate.now().minusDays(1));

        Enfant enfant = new Enfant();
        when(enfantRepo.findById(1L)).thenReturn(Optional.of(enfant));

        assertThatThrownBy(() -> service.addCondition(1L, condition))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("passe");

        verify(conditionRepo, never()).save(any());
    }

    @Test
    void addCondition_chronique_forceDateFinNull() {
        ConditionSanitaire condition = new ConditionSanitaire();
        condition.setType(TypeConditionSanitaire.MALADIE_CHRONIQUE);
        condition.setNomCondition("Asthme");
        condition.setDateDebut(LocalDate.now().plusDays(1));
        condition.setDateFin(LocalDate.now().plusDays(10));

        Enfant enfant = new Enfant();
        when(enfantRepo.findById(1L)).thenReturn(Optional.of(enfant));
        when(conditionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.addCondition(1L, condition);

        verify(conditionRepo).save(any());
        // l'objet passé au repo est le même, donc on vérifie le champ modifié
        assert condition.getDateFin() == null;
    }

    @Test
    void addCondition_temporaire_refuseSansDateFin() {
        ConditionSanitaire condition = new ConditionSanitaire();
        condition.setType(TypeConditionSanitaire.MALADIE_TEMPORAIRE);
        condition.setNomCondition("Grippe");
        condition.setDateDebut(LocalDate.now().plusDays(1));
        condition.setDateFin(null);

        Enfant enfant = new Enfant();
        when(enfantRepo.findById(1L)).thenReturn(Optional.of(enfant));

        assertThatThrownBy(() -> service.addCondition(1L, condition))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("date de fin");

        verify(conditionRepo, never()).save(any());
    }

    @Test
    void updateCondition_chronique_forceDateFinNull() {
        ConditionSanitaire existing = new ConditionSanitaire();
        existing.setType(TypeConditionSanitaire.MALADIE_TEMPORAIRE);
        existing.setNomCondition("Test");
        existing.setDateDebut(LocalDate.now().plusDays(1));
        existing.setDateFin(LocalDate.now().plusDays(2));

        when(conditionRepo.findById(5L)).thenReturn(Optional.of(existing));
        when(conditionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConditionSanitaire updated = new ConditionSanitaire();
        updated.setType(TypeConditionSanitaire.MALADIE_CHRONIQUE);
        updated.setNomCondition("Asthme");
        updated.setDateDebut(LocalDate.now().plusDays(1));
        updated.setDateFin(LocalDate.now().plusDays(10));

        service.updateCondition(5L, updated);

        verify(conditionRepo).save(existing);
        assert existing.getDateFin() == null;
    }
}
