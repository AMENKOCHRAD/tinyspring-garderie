package com.tinyspring.garderie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private long totalSalles;
    private long totalClasses;
    private long totalGroupes;
    private long totalEnfantsActifs;
    private double globalOccupancyRate;
    
    // Pour ApexCharts (Donut Language)
    private Map<String, Long> repartitionsLangues;
    
    // Pour Histogramme ou Analyse Salles
    private Map<String, Long> enfantsParSalle;
}
