package com.tinyspring.garderie.dto.boutique;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardRecentCommandeDto {
    private Long id;
    private LocalDateTime dateCommande;
    private String statut;
    private Double montantTotal;
    private String userNom;
    private String userEmail;
}