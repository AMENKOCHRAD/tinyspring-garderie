package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.TraitementUpdateDto;
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
import org.springframework.core.io.ByteArrayResource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraitementServiceImplParentActionsTest {

    @Mock TraitementRepository traitementRepository;
    @Mock ConditionSanitaireRepository conditionSanitaireRepository;
    @Mock UserRepository userRepository;
    @Mock OrdonnanceStorageService ordonnanceStorageService;
    @Mock TraitementAutoValidationService autoValidationService;
    @Mock RealtimeNotificationService realtimeNotificationService;
    @Mock TraitementValidationHistoryService validationHistoryService;
    @Mock TraitementMapper traitementMapper;

    @InjectMocks
    TraitementServiceImpl service;

    @Test
    void supprimerTraitementParParent_refuseSiPasProprietaire() {
        when(userRepository.findByEmailIgnoreCase("p@test.com")).thenReturn(Optional.of(new User()));

        Traitement t = new Traitement();
        t.setId(5L);
        ConditionSanitaire cs = new ConditionSanitaire();
        Enfant enfant = new Enfant();
        User parent = new User();
        parent.setEmail("autre@test.com");
        enfant.setParent(parent);
        cs.setEnfant(enfant);
        t.setConditionSanitaire(cs);

        when(traitementRepository.findById(5L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.supprimerTraitementParParent("p@test.com", 5L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Acces interdit");

        verify(traitementRepository, never()).delete(any());
    }

    @Test
    void supprimerTraitementParParent_okSiProprietaire() {
        when(userRepository.findByEmailIgnoreCase("p@test.com")).thenReturn(Optional.of(new User()));

        Traitement t = new Traitement();
        t.setId(5L);
        ConditionSanitaire cs = new ConditionSanitaire();
        Enfant enfant = new Enfant();
        User parent = new User();
        parent.setEmail("p@test.com");
        enfant.setParent(parent);
        cs.setEnfant(enfant);
        t.setConditionSanitaire(cs);
        t.setStatut(StatutTraitement.EN_ATTENTE_VALIDATION);

        when(traitementRepository.findById(5L)).thenReturn(Optional.of(t));

        service.supprimerTraitementParParent("p@test.com", 5L);

        verify(traitementRepository).delete(t);
    }

    @Test
    void chargerOrdonnanceResourceParParent_refuseSiPasProprietaire() {
        Traitement t = new Traitement();
        t.setId(5L);
        t.setOrdonnance("x.pdf");
        ConditionSanitaire cs = new ConditionSanitaire();
        Enfant enfant = new Enfant();
        User parent = new User();
        parent.setEmail("autre@test.com");
        enfant.setParent(parent);
        cs.setEnfant(enfant);
        t.setConditionSanitaire(cs);

        when(traitementRepository.findById(5L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.chargerOrdonnanceResourceParParent("p@test.com", 5L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Acces interdit");

        verifyNoInteractions(ordonnanceStorageService);
    }

    @Test
    void chargerOrdonnanceResourceAdmin_chargeDirectement() {
        Traitement t = new Traitement();
        t.setId(5L);
        t.setOrdonnance("x.pdf");
        when(traitementRepository.findById(5L)).thenReturn(Optional.of(t));
        ByteArrayResource res = new ByteArrayResource(new byte[]{1});
        when(ordonnanceStorageService.loadAsResource("x.pdf")).thenReturn(res);

        service.chargerOrdonnanceResourceAdmin(5L);

        verify(ordonnanceStorageService).loadAsResource("x.pdf");
    }

    @Test
    void modifierTraitementParParent_refuseSiAnnule() {
        when(userRepository.findByEmailIgnoreCase("p@test.com")).thenReturn(Optional.of(new User()));

        Traitement t = new Traitement();
        t.setId(5L);
        t.setStatut(StatutTraitement.ANNULE);
        ConditionSanitaire cs = new ConditionSanitaire();
        Enfant enfant = new Enfant();
        User parent = new User();
        parent.setEmail("p@test.com");
        enfant.setParent(parent);
        cs.setEnfant(enfant);
        t.setConditionSanitaire(cs);

        when(traitementRepository.findById(5L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.modifierTraitementParParent("p@test.com", 5L, new TraitementUpdateDto()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("annule");

        verify(traitementRepository, never()).save(any());
    }
}
