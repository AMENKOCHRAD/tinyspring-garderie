package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.EnfantDTO;
import com.tinyspring.garderie.dto.EnfantResponseDTO;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.service.EnfantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnfantControllerTest {

    @Mock
    EnfantService enfantService;

    @InjectMocks
    EnfantController controller;

    @Test
    void getAllEnfants_retourneListeDuService() {
        when(enfantService.getAllEnfants()).thenReturn(List.of(new EnfantResponseDTO(), new EnfantResponseDTO()));
        assertThat(controller.getAllEnfants()).hasSize(2);
        verify(enfantService).getAllEnfants();
    }

    @Test
    void archiverEnfant_wrapResponseEntityOk() {
        Enfant e = new Enfant();
        when(enfantService.archiverEnfant(5L)).thenReturn(e);
        var resp = controller.archiverEnfant(5L);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isSameAs(e);
    }

    @Test
    void ajouterEnfant_delegueAuService() {
        EnfantDTO dto = new EnfantDTO();
        Enfant saved = new Enfant();
        when(enfantService.ajouterEnfant(dto)).thenReturn(saved);
        assertThat(controller.ajouterEnfant(dto)).isSameAs(saved);
        verify(enfantService).ajouterEnfant(dto);
    }
}

