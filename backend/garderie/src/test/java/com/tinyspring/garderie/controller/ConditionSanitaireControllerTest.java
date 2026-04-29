package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.entity.ConditionSanitaire;
import com.tinyspring.garderie.service.ConditionSanitaireService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConditionSanitaireControllerTest {

    @Mock
    ConditionSanitaireService service;

    @InjectMocks
    ConditionSanitaireController controller;

    @Test
    void getConditions_retourneOkEtListe() {
        when(service.getConditionsByEnfant(2L)).thenReturn(List.of(new ConditionSanitaire()));
        var resp = controller.getConditions(2L);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).hasSize(1);
    }

    @Test
    void deleteCondition_retourne204() {
        var resp = controller.deleteCondition(9L);
        assertThat(resp.getStatusCode().value()).isEqualTo(204);
        verify(service).deleteCondition(9L);
    }
}

