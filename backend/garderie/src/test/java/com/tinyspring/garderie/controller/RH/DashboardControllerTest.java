package com.tinyspring.garderie.controller.RH;

import com.tinyspring.garderie.dto.RH.DashboardStatsDTO;
import com.tinyspring.garderie.service.RH.IDashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests DashboardController")
class DashboardControllerTest {

    @Mock  private IDashboardService dashboardService;
    @InjectMocks private DashboardController controller;

    @Test
    @DisplayName("getStats — doit retourner 200 avec les statistiques")
    void getStats_doitRetourner200() {
        DashboardStatsDTO stats = DashboardStatsDTO.builder()
                .totalAnimatrices(5L).animatricesActives(4L)
                .totalAbsences(10L).totalFormations(8L)
                .dernieresDemandesEnAttente(List.of())
                .dernieresAnimatrices(List.of())
                .build();

        when(dashboardService.getStats()).thenReturn(stats);

        ResponseEntity<DashboardStatsDTO> response = controller.getStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTotalAnimatrices()).isEqualTo(5L);
        assertThat(response.getBody().getTotalAbsences()).isEqualTo(10L);
        assertThat(response.getBody().getTotalFormations()).isEqualTo(8L);
    }
}