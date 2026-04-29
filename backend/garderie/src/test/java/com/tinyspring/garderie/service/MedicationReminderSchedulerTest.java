package com.tinyspring.garderie.service;

import com.tinyspring.garderie.entity.ConditionSanitaire;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.StatutTraitement;
import com.tinyspring.garderie.entity.Traitement;
import com.tinyspring.garderie.repository.PriseTraitementRepository;
import com.tinyspring.garderie.repository.TraitementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicationReminderSchedulerTest {

    @Mock
    TraitementRepository traitementRepository;

    @Mock
    PriseTraitementRepository priseTraitementRepository;

    @Mock
    RealtimeNotificationService realtimeNotificationService;

    @InjectMocks
    MedicationReminderScheduler scheduler;

    @Test
    void tick_envoieNotificationQuandHeureDueEtPasDejaDonne() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 29, 9, 0); // Wednesday
        Traitement t = traitementAvecEnfant("Nom", "Prenom");
        t.setId(10L);
        t.setStatut(StatutTraitement.VALIDE);
        t.setNomTraitement("Sirop");
        t.setHeuresPrises(List.of("09:00", "12:00"));

        when(traitementRepository.findActifsPourDate(anyList(), eq(now.toLocalDate()))).thenReturn(List.of(t));
        when(priseTraitementRepository.findByTraitementIdAndDatePriseAndHeurePrevue(10L, now.toLocalDate(), "09:00"))
                .thenReturn(Optional.empty());

        try (MockedStatic<LocalDateTime> mocked = mockStatic(LocalDateTime.class)) {
            mocked.when(LocalDateTime::now).thenReturn(now);
            scheduler.tick();
        }

        verify(realtimeNotificationService).notifyAnimatricesDoseDue(
                eq(1L),
                eq("Nom"),
                eq("Prenom"),
                eq(10L),
                eq("Sirop"),
                eq(LocalDate.of(2026, 4, 29).toString()),
                eq("09:00")
        );
    }

    @Test
    void tick_neFaitRienLeDimanche() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 3, 9, 0); // Sunday
        try (MockedStatic<LocalDateTime> mocked = mockStatic(LocalDateTime.class)) {
            mocked.when(LocalDateTime::now).thenReturn(now);
            scheduler.tick();
        }
        verifyNoInteractions(traitementRepository);
        verifyNoInteractions(realtimeNotificationService);
    }

    @Test
    void tick_neFaitRienLeSamediApres1230() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 2, 13, 0); // Saturday 13:00
        try (MockedStatic<LocalDateTime> mocked = mockStatic(LocalDateTime.class)) {
            mocked.when(LocalDateTime::now).thenReturn(now);
            scheduler.tick();
        }
        verifyNoInteractions(traitementRepository);
        verifyNoInteractions(realtimeNotificationService);
    }

    private static Traitement traitementAvecEnfant(String nom, String prenom) {
        Enfant enfant = new Enfant();
        enfant.setId(1L);
        enfant.setNom(nom);
        enfant.setPrenom(prenom);
        ConditionSanitaire cs = new ConditionSanitaire();
        cs.setEnfant(enfant);
        Traitement t = new Traitement();
        t.setConditionSanitaire(cs);
        return t;
    }
}
