package com.tinyspring.garderie.dto.boutique;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorieDto {

    private Long id;
    private String nom;
    private String description;
    private String imageUrl;
    private int nombreProduits; // calculé côté service
}
