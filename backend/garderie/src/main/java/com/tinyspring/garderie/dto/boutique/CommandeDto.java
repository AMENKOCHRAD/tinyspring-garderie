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
    private String paymentStatus;
    private Double montantTotal;
    private String adresseLivraison;
    private String stripeSessionId;
    private String stripePaymentIntentId;
    private Long userId;
    private String userNom;
    private String userEmail;
    private List<CommandeItemDto> items; // ✅ avec quantités
}