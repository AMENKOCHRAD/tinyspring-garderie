package com.tinyspring.garderie.dto.boutique;

import lombok.*;
import java.util.List;

// DTO utilisé pour la création d'une commande (reçu depuis le frontend)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandeRequest {

    private String adresseLivraison;
    private Long userId;
    private List<Long> produitIds; // liste des IDs produits commandés
}
