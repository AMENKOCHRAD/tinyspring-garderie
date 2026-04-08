package com.tinyspring.garderie.dto.boutique;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDto {

    private long totalProduits;
    private long produitsEnStock;
    private long produitsRupture;
    private long produitsStockFaible;

    private long totalCategories;

    private long totalCommandes;
    private long commandesEnAttente;
    private long commandesConfirmees;
    private long commandesExpediees;
    private long commandesLivrees;
    private long commandesAnnulees;

    private double montantTotalVentes;

    private List<DashboardRecentCommandeDto> dernieresCommandes;
    private List<DashboardTopProduitDto> topProduits;
    private List<DashboardMonthlySalesDto> ventesParMois;
}