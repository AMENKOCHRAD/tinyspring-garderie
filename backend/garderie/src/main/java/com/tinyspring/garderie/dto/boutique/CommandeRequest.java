package com.tinyspring.garderie.dto.boutique;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandeRequest {
    private String adresseLivraison;
    private Long userId;
    private List<CommandeItemRequest> items; // ✅ avec quantités
}
