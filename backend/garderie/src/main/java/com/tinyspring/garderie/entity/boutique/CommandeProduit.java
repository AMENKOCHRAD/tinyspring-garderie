package com.tinyspring.garderie.entity.boutique;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "commande_produit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandeProduit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id", nullable = false)
    private Commande commande;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(nullable = false)
    private Integer quantite;

    // Snapshot du prix au moment de la commande
    @Column(name = "prix_unitaire", nullable = false)
    private Double prixUnitaire;

    public Double getSousTotal() {
        return this.quantite * this.prixUnitaire;
    }
}
