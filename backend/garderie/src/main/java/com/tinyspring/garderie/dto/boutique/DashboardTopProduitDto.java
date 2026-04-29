package com.tinyspring.garderie.dto.boutique;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardTopProduitDto {
    private Long id;
    private String nom;
    private String imageUrl;
    private Long totalCommandes;
}