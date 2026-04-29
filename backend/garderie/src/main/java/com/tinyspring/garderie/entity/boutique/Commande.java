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

    // Statuts : PENDING | CONFIRMEE | EXPEDIEE | LIVREE | ANNULEE
    @Column(nullable = false)
    private String statut;

    // Statut paiement Stripe : PENDING | PAID | FAILED | CANCELED
    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "montant_total", nullable = false)
    private Double montantTotal;

    @Column(name = "adresse_livraison", nullable = false)
    private String adresseLivraison;

    @Column(name = "stripe_session_id")
    private String stripeSessionId;

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    @Column(name = "mode_paiement")
    private String modePaiement = "STRIPE";

    @Column(name = "paiement_especes_demande")
    private Boolean paiementEspecesDemande = false;

    @Column(name = "date_choix_especes")
    private LocalDateTime dateChoixEspeces;

    @Column(name = "token_action", unique = true)
    private String tokenAction;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ✅ Remplace @ManyToMany — permet de stocker la quantité par produit
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CommandeProduit> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.dateCommande == null)  this.dateCommande = LocalDateTime.now();
        if (this.statut == null)        this.statut = "PENDING";
        if (this.paymentStatus == null) this.paymentStatus = "PENDING";
    }
}
