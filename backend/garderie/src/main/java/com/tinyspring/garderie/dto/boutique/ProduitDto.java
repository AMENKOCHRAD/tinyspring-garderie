package com.tinyspring.garderie.dto.boutique;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProduitDto {

    private Long id;
    private String nom;
    private String description;
    private Double prix;
    private Integer stock;
    private String imageUrl;

    // On n'expose pas l'objet Categorie entier → juste id + nom
    private Long categorieId;
    private String categorieNom;
}
