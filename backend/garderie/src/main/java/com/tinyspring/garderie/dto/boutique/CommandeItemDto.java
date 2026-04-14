package com.tinyspring.garderie.dto.boutique;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandeItemDto {
    private Long produitId;
    private String produitNom;
    private String produitImageUrl;
    private Integer quantite;
    private Double prixUnitaire;
    private Double sousTotal;
}
