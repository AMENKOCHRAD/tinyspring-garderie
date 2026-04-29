package com.tinyspring.garderie.controller.boutique;

import com.tinyspring.garderie.dto.boutique.DashboardStatsDto;
import com.tinyspring.garderie.service.boutique.DashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests DashboardController")
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

    @Test
    @DisplayName("getStats() retourne 200 avec les statistiques")
    void getStats_shouldReturn200WithDashboardStats() {
        DashboardStatsDto stats = DashboardStatsDto.builder()
                .totalProduits(12L)
                .produitsEnStock(9L)
                .produitsRupture(1L)
                .produitsStockFaible(2L)
                .totalCategories(4L)
                .totalCommandes(20L)
                .commandesEnAttente(3L)
                .commandesConfirmees(8L)
                .commandesExpediees(5L)
                .commandesLivrees(3L)
                .commandesAnnulees(1L)
                .montantTotalVentes(499.90)
                .dernieresCommandes(List.of())
                .topProduits(List.of())
                .ventesParMois(List.of())
                .build();
        when(dashboardService.getStats()).thenReturn(stats);

        ResponseEntity<DashboardStatsDto> response = dashboardController.getStats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(stats);
    }
}
