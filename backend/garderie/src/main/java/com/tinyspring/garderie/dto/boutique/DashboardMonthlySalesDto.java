package com.tinyspring.garderie.dto.boutique;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardMonthlySalesDto {
    private String mois;
    private Double montant;
}