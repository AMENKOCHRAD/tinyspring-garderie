package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.TraitementUpdateDto;
import com.tinyspring.garderie.entity.Traitement;
import com.tinyspring.garderie.service.TraitementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParentTraitementControllerTest {

    @Mock
    TraitementService traitementService;

    @InjectMocks
    ParentTraitementController controller;

    @Test
    void supprimer_retourne204() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("p@test.com");

        var resp = controller.supprimer(5L, auth);

        assertThat(resp.getStatusCode().value()).isEqualTo(204);
        verify(traitementService).supprimerTraitementParParent("p@test.com", 5L);
    }

    @Test
    void modifier_utiliseEmailParent() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("p@test.com");

        TraitementUpdateDto payload = new TraitementUpdateDto();
        when(traitementService.modifierTraitementParParent("p@test.com", 7L, payload)).thenReturn(new Traitement());

        var resp = controller.modifier(7L, payload, auth);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(traitementService).modifierTraitementParParent("p@test.com", 7L, payload);
    }
}

