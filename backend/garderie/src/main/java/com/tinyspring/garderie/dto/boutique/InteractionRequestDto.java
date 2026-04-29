package com.tinyspring.garderie.dto.boutique;

import lombok.Data;

@Data
public class InteractionRequestDto {
    private Long produitId;
    // VUE_3S | VUE_10S | VUE_30S | CLIC_DETAIL | RECHERCHE | AJOUT_PANIER | COMMANDE | ANNULATION
    private String typeInteraction;
}