package com.tinyspring.garderie.service.boutique;

import com.tinyspring.garderie.dto.boutique.DashboardStatsDto;
import com.tinyspring.garderie.repository.boutique.CategorieRepository;
import com.tinyspring.garderie.repository.boutique.CommandeRepository;
import com.tinyspring.garderie.repository.boutique.ProduitRepository;
import org.springframework.stereotype.Service;

import com.tinyspring.garderie.dto.boutique.DashboardMonthlySalesDto;
import com.tinyspring.garderie.dto.boutique.DashboardRecentCommandeDto;
import com.tinyspring.garderie.dto.boutique.DashboardTopProduitDto;
import com.tinyspring.garderie.entity.boutique.Commande;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@Service
public class DashboardService {

    private final ProduitRepository produitRepository;
    private final CategorieRepository categorieRepository;
    private final CommandeRepository commandeRepository;

    public DashboardService(
            ProduitRepository produitRepository,
            CategorieRepository categorieRepository,
            CommandeRepository commandeRepository
    ) {
        this.produitRepository = produitRepository;
        this.categorieRepository = categorieRepository;
        this.commandeRepository = commandeRepository;
    }

    public DashboardStatsDto getStats() {
        Double montantTotal = commandeRepository.sumMontantTotalCommandesValides();



        List<DashboardRecentCommandeDto> dernieresCommandes = commandeRepository
                .findAllByOrderByDateCommandeDesc(PageRequest.of(0, 5))
                .stream()
                .map(this::toRecentCommandeDto)
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
    private DashboardRecentCommandeDto toRecentCommandeDto(Commande commande) {
        return DashboardRecentCommandeDto.builder()
                .id(commande.getId())
                .dateCommande(commande.getDateCommande())
                .statut(commande.getStatut())
                .montantTotal(commande.getMontantTotal())
                .userNom(commande.getUser() != null ? commande.getUser().getNom() : null)
                .userEmail(commande.getUser() != null ? commande.getUser().getEmail() : null)
                .build();
    }
}