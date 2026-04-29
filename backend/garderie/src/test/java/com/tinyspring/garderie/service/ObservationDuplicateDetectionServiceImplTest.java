package com.tinyspring.garderie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyspring.garderie.entity.ObservationEnfant;
import com.tinyspring.garderie.entity.ObservationType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ObservationDuplicateDetectionServiceImplTest {

    private final ObservationDuplicateDetectionServiceImpl service =
            new ObservationDuplicateDetectionServiceImpl(new ObjectMapper());

    @Test
    void predictDuplicate_retourneTrue_siMemeType_etTexteQuasiIdentique() {
        ObservationEnfant a = new ObservationEnfant();
        a.setType(ObservationType.SANTE);
        a.setTitre("Fievre");
        a.setDescription("Fievre 38 5 depuis ce matin");
        a.setCreeLe(LocalDateTime.now());

        ObservationEnfant b = new ObservationEnfant();
        b.setType(ObservationType.SANTE);
        b.setTitre("Fievre");
        b.setDescription("Fievre 38 5 depuis ce matin");
        b.setCreeLe(LocalDateTime.now());

        var res = service.predictDuplicate(a, b);
        assertThat(res.duplicate()).isTrue();
        assertThat(res.score()).isGreaterThanOrEqualTo(0.85);
    }

    @Test
    void predictDuplicate_retourneFalse_siTypeDifferent() {
        ObservationEnfant a = new ObservationEnfant();
        a.setType(ObservationType.SANTE);
        a.setTitre("Fièvre");
        a.setDescription("38.5");

        ObservationEnfant b = new ObservationEnfant();
        b.setType(ObservationType.SOMMEIL);
        b.setTitre("Fièvre");
        b.setDescription("38.5");

        var res = service.predictDuplicate(a, b);
        assertThat(res.duplicate()).isFalse();
    }
}
