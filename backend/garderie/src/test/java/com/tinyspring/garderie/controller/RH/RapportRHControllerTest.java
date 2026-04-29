package com.tinyspring.garderie.controller.RH;

import com.tinyspring.garderie.dto.RH.RapportRHDTO;
import com.tinyspring.garderie.dto.RH.RapportRequestDTO;
import com.tinyspring.garderie.service.RH.IRapportRHService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests RapportRHController")
class RapportRHControllerTest {

    @Mock  private IRapportRHService rapportRHService;
    @InjectMocks private RapportRHController controller;

    private RapportRHDTO rapportDTO;

    @BeforeEach
    void setUp() {
        rapportDTO = RapportRHDTO.builder()
                .id(1L).question("Rapport mensuel")
                .typeRapport("MENSUEL").periode("Mois en cours")
                .contenu("Contenu généré").dateGeneration(LocalDateTime.now()).build();
    }

    @Test @DisplayName("genererRapport — 200")
    void genererRapport() {
        when(rapportRHService.genererRapport(any())).thenReturn(rapportDTO);
        RapportRequestDTO req = new RapportRequestDTO("Rapport mensuel");
        ResponseEntity<RapportRHDTO> r = controller.genererRapport(req);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().getTypeRapport()).isEqualTo("MENSUEL");
    }

    @Test @DisplayName("getTousLesRapports — 200")
    void getTousLesRapports() {
        when(rapportRHService.getTousLesRapports()).thenReturn(List.of(rapportDTO));
        ResponseEntity<List<RapportRHDTO>> r = controller.getTousLesRapports();
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).hasSize(1);
    }

    @Test @DisplayName("getRapportById — 200")
    void getRapportById() {
        when(rapportRHService.getRapportById(1L)).thenReturn(rapportDTO);
        ResponseEntity<RapportRHDTO> r = controller.getRapportById(1L);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody().getId()).isEqualTo(1L);
    }
}
