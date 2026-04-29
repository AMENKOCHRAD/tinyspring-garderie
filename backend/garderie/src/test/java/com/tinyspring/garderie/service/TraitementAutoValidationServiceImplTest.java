package com.tinyspring.garderie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyspring.garderie.entity.Traitement;
import com.tinyspring.garderie.repository.ConditionSanitaireRepository;
import com.tinyspring.garderie.repository.ObservationEnfantRepository;
import com.tinyspring.garderie.repository.TraitementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TraitementAutoValidationServiceImplTest {

    @Mock
    TraitementRepository traitementRepository;

    @Mock
    ConditionSanitaireRepository conditionSanitaireRepository;

    @Mock
    ObservationEnfantRepository observationEnfantRepository;

    private TraitementAutoValidationServiceImpl service() {
        return new TraitementAutoValidationServiceImpl(
            traitementRepository,
            conditionSanitaireRepository,
            observationEnfantRepository,
            new ObjectMapper()
        );
    }

    @Test
    void evaluer_refuseParRegles_siInfosManquantes() {
        Traitement t = new Traitement();
        t.setNomTraitement("Sirop");
        t.setOrdonnance(" "); // absente
        t.setDateDebut(LocalDate.now().plusDays(1));

        var res = service().evaluer(t);

        assertThat(res.decision).isEqualTo(TraitementAutoValidationService.Decision.REFUSE);
        assertThat(res.source).isEqualTo("RULES");
        assertThat(res.confiance).isEqualTo(1.0);
        assertThat(res.facteurs).isNotEmpty();
    }
}
