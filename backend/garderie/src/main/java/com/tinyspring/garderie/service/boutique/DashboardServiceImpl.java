package com.tinyspring.garderie.service.boutique;

import com.tinyspring.garderie.dto.boutique.*;
import com.tinyspring.garderie.mapper.boutique.DashboardMapper;
import com.tinyspring.garderie.repository.boutique.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ProduitRepository produitRepository;
    private final CategorieRepository categorieRepository;
    private final CommandeRepository commandeRepository;
    private final DashboardMapper dashboardMapper;

    @Override
    public DashboardStatsDto getStats() {
        Double montantTotal = commandeRepository.sumMontantTotalCommandesValides();

        List<DashboardRecentCommandeDto> dernieresCommandes = commandeRepository
                .findAllByOrderByDateCommandeDesc(PageRequest.of(0, 5))
                .stream()
                .map(dashboardMapper::toRecentCommandeDto)
                .toList();

        List<DashboardTopProduitDto> topProduits = produitRepository
                .findTopProduitsByCommandes(PageRequest.of(0, 5))
                .stream()
                .map(row -> DashboardTopProduitDto.builder()
                        .id((Long) row[0])
                        .nom((String) row[1])
                        .imageUrl((String) row[2])
                        .totalCommandes((Long) row[3])
                        .build())
                .toList();

        List<DashboardMonthlySalesDto> ventesParMois = commandeRepository
                .sumMontantTotalGroupByMonth()
                .stream()
                .map(row -> DashboardMonthlySalesDto.builder()
                        .mois((String) row[0])
                        .montant(((Number) row[1]).doubleValue())
                        .build())
                .toList();

        return DashboardStatsDto.builder()
                .totalProduits(produitRepository.count())
                .produitsEnStock(produitRepository.countByStockGreaterThan(0))
                .produitsRupture(produitRepository.countByStockEquals(0))
                .produitsStockFaible(produitRepository.countLowStockProduits())
                .totalCategories(categorieRepository.count())
                .totalCommandes(commandeRepository.count())
                .commandesEnAttente(commandeRepository.countByStatut("EN_ATTENTE"))
                .commandesConfirmees(commandeRepository.countByStatut("CONFIRMEE"))
                .commandesExpediees(commandeRepository.countByStatut("EXPEDIEE"))
                .commandesLivrees(commandeRepository.countByStatut("LIVREE"))
                .commandesAnnulees(commandeRepository.countByStatut("ANNULEE"))
                .montantTotalVentes(montantTotal != null ? montantTotal : 0.0)
                .dernieresCommandes(dernieresCommandes)
                .topProduits(topProduits)
                .ventesParMois(ventesParMois)
                .build();
    }
}