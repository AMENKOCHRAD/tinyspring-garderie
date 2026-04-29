package com.tinyspring.garderie.service.boutique;

import com.tinyspring.garderie.dto.boutique.*;
import com.tinyspring.garderie.entity.boutique.Commande;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.mapper.boutique.DashboardMapper;
import com.tinyspring.garderie.repository.boutique.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests DashboardService")
class DashboardServiceTest {

    @Mock private ProduitRepository produitRepository;
    @Mock private CategorieRepository categorieRepository;
    @Mock private CommandeRepository commandeRepository;
    @Mock private DashboardMapper dashboardMapper;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    // ← déclaré comme champ de classe
    private User user;
    private Commande commande;
    private DashboardRecentCommandeDto recentDto;

    @BeforeEach
    void setUp() {
        user = new User("Parent Test", "parent@test.com", "password123", true, null);

        commande = Commande.builder()
                .id(1L).statut("CONFIRMEE")
                .montantTotal(99.99)
                .dateCommande(LocalDateTime.now())
                .user(user)
                .build();

        recentDto = DashboardRecentCommandeDto.builder()
                .id(1L).statut("CONFIRMEE")
                .montantTotal(99.99)
                .userNom("Parent Test")
                .userEmail("parent@test.com")
                .build();
    }

    @Test
    @DisplayName("getStats() retourne les statistiques complètes")
    void getStats_shouldReturnCompleteStats() {
        when(commandeRepository.sumMontantTotalCommandesValides()).thenReturn(500.0);
        when(commandeRepository.findAllByOrderByDateCommandeDesc(any()))
                .thenReturn(List.of(commande));
        when(dashboardMapper.toRecentCommandeDto(commande)).thenReturn(recentDto);
        when(produitRepository.findTopProduitsByCommandes(any())).thenReturn(List.of());
        when(commandeRepository.sumMontantTotalGroupByMonth()).thenReturn(List.of());
        when(produitRepository.count()).thenReturn(52L);
        when(produitRepository.countByStockGreaterThan(0)).thenReturn(51L);
        when(produitRepository.countByStockEquals(0)).thenReturn(1L);
        when(produitRepository.countLowStockProduits()).thenReturn(1L);
        when(categorieRepository.count()).thenReturn(7L);
        when(commandeRepository.count()).thenReturn(10L);
        when(commandeRepository.countByStatut(any())).thenReturn(0L);

        DashboardStatsDto result = dashboardService.getStats();

        assertThat(result).isNotNull();
        assertThat(result.getTotalProduits()).isEqualTo(52L);
        assertThat(result.getProduitsEnStock()).isEqualTo(51L);
        assertThat(result.getProduitsRupture()).isEqualTo(1L);
        assertThat(result.getTotalCategories()).isEqualTo(7L);
        assertThat(result.getTotalCommandes()).isEqualTo(10L);
        assertThat(result.getMontantTotalVentes()).isEqualTo(500.0);
        assertThat(result.getDernieresCommandes()).hasSize(1);
    }

    @Test
    @DisplayName("getStats() retourne 0.0 si montant total null")
    void getStats_shouldReturn0_whenMontantNull() {
        when(commandeRepository.sumMontantTotalCommandesValides()).thenReturn(null);
        when(commandeRepository.findAllByOrderByDateCommandeDesc(any()))
                .thenReturn(List.of());
        when(produitRepository.findTopProduitsByCommandes(any())).thenReturn(List.of());
        when(commandeRepository.sumMontantTotalGroupByMonth()).thenReturn(List.of());
        when(produitRepository.count()).thenReturn(0L);
        when(produitRepository.countByStockGreaterThan(0)).thenReturn(0L);
        when(produitRepository.countByStockEquals(0)).thenReturn(0L);
        when(produitRepository.countLowStockProduits()).thenReturn(0L);
        when(categorieRepository.count()).thenReturn(0L);
        when(commandeRepository.count()).thenReturn(0L);
        when(commandeRepository.countByStatut(any())).thenReturn(0L);

        DashboardStatsDto result = dashboardService.getStats();

        assertThat(result.getMontantTotalVentes()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getStats() mappe correctement les dernières commandes")
    void getStats_shouldMapDernieresCommandes() {
        when(commandeRepository.sumMontantTotalCommandesValides()).thenReturn(100.0);
        when(commandeRepository.findAllByOrderByDateCommandeDesc(any()))
                .thenReturn(List.of(commande));
        when(dashboardMapper.toRecentCommandeDto(commande)).thenReturn(recentDto);
        when(produitRepository.findTopProduitsByCommandes(any())).thenReturn(List.of());
        when(commandeRepository.sumMontantTotalGroupByMonth()).thenReturn(List.of());
        when(produitRepository.count()).thenReturn(0L);
        when(produitRepository.countByStockGreaterThan(0)).thenReturn(0L);
        when(produitRepository.countByStockEquals(0)).thenReturn(0L);
        when(produitRepository.countLowStockProduits()).thenReturn(0L);
        when(categorieRepository.count()).thenReturn(0L);
        when(commandeRepository.count()).thenReturn(0L);
        when(commandeRepository.countByStatut(any())).thenReturn(0L);

        DashboardStatsDto result = dashboardService.getStats();

        assertThat(result.getDernieresCommandes()).hasSize(1);
        assertThat(result.getDernieresCommandes().get(0).getUserNom())
                .isEqualTo("Parent Test");
    }
}