package com.tinyspring.garderie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyspring.garderie.dto.TraitementValidationEventDto;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.StatutTraitement;
import com.tinyspring.garderie.entity.Traitement;
import com.tinyspring.garderie.entity.TraitementValidationEvent;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.repository.TraitementValidationEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraitementValidationHistoryServiceImplTest {

    @Mock
    TraitementValidationEventRepository repository;

    private TraitementValidationHistoryServiceImpl service() {
        return new TraitementValidationHistoryServiceImpl(repository, new ObjectMapper());
    }

    @Test
    void recordSystemDecision_sauvegardeEvent_siTraitementIdEtDecision() {
        Traitement t = new Traitement();
        t.setId(10L);
        t.setNomTraitement("Sirop");
        t.setStatut(StatutTraitement.VALIDE);

        TraitementAutoValidationService.Result res = new TraitementAutoValidationService.Result();
        res.decision = TraitementAutoValidationService.Decision.ACCEPTE;
        res.confiance = 0.9;
        res.source = "ML";
        res.note = "OK";
        res.facteurs = List.of("f1", "f2");

        service().recordSystemDecision(t, res);

        ArgumentCaptor<TraitementValidationEvent> captor = ArgumentCaptor.forClass(TraitementValidationEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getDecision()).isEqualTo("ACCEPTE");
        assertThat(captor.getValue().getSource()).isEqualTo("ML");
        assertThat(captor.getValue().getCreeParEmail()).isEqualTo("SYSTEM");
        assertThat(captor.getValue().getFacteursJson()).contains("f1");
    }

    @Test
    void getHistory_mappeDetailsEnfantEtParent() {
        Traitement t = new Traitement();
        t.setId(1L);
        t.setNomTraitement("Sirop");
        t.setStatut(StatutTraitement.VALIDE);

        var cs = new com.tinyspring.garderie.entity.ConditionSanitaire();
        cs.setNomCondition("Asthme");
        cs.setType(com.tinyspring.garderie.entity.TypeConditionSanitaire.MALADIE_CHRONIQUE);

        User parent = new User();
        parent.setNom("Parent");
        Enfant enfant = new Enfant();
        enfant.setId(7L);
        enfant.setNom("Nom");
        enfant.setPrenom("Prenom");
        enfant.setParent(parent);
        cs.setEnfant(enfant);

        t.setConditionSanitaire(cs);

        TraitementValidationEvent e = new TraitementValidationEvent();
        e.setTraitement(t);
        e.setDecision("VALIDE_ADMIN");
        e.setSource("ADMIN");
        e.setNote("note");
        e.setFacteursJson(null);

        when(repository.findTop100ByTraitementIdOrderByCreeLeDesc(1L)).thenReturn(List.of(e));

        List<TraitementValidationEventDto> out = service().getHistory(1L);
        assertThat(out).hasSize(1);
        assertThat(out.get(0).nomTraitement).isEqualTo("Sirop");
        assertThat(out.get(0).nomCondition).isEqualTo("Asthme");
        assertThat(out.get(0).nomEnfant).isEqualTo("Nom");
        assertThat(out.get(0).prenomEnfant).isEqualTo("Prenom");
        assertThat(out.get(0).nomParent).isEqualTo("Parent");
    }
}
