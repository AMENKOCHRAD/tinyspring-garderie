package com.tinyspring.garderie.dto.boutique;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandeItemRequest {
    private Long produitId;
    private Integer quantite;
}
