package com.tinyspring.garderie.entity.boutique;

import com.tinyspring.garderie.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "commandes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_commande", nullable = false)
    private LocalDateTime dateCommande;

    // Valeurs possibles : EN_ATTENTE | CONFIRMEE | EXPEDIEE | LIVREE | ANNULEE
    @Column(nullable = false)
    private String statut;

    @Column(name = "montant_total", nullable = false)
    private Double montantTotal;

    @Column(name = "adresse_livraison", nullable = false)
    private String adresseLivraison;

    // Lié à l'utilisateur connecté du projet existant
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "commande_produit",
            joinColumns = @JoinColumn(name = "commande_id"),
            inverseJoinColumns = @JoinColumn(name = "produit_id")
    )
    @Builder.Default
    private List<Produit> produits = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.dateCommande == null) this.dateCommande = LocalDateTime.now();
        if (this.statut == null)       this.statut = "EN_ATTENTE";
    }
}
