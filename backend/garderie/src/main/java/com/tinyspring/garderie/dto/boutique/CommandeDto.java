package com.tinyspring.garderie.dto.boutique;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandeDto {

    private Long id;
    private LocalDateTime dateCommande;
    private String statut;
    private Double montantTotal;
    private String adresseLivraison;

    // Infos user (pas l'objet User entier)
    private Long userId;
    private String userNom;
    private String userEmail;

    // Liste des produits dans la commande (résumé)
    private List<ProduitDto> produits;
}
