package com.tinyspring.garderie.entity.boutique;

import com.tinyspring.garderie.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_interactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    // VUE_3S | VUE_10S | VUE_30S | CLIC_DETAIL | RECHERCHE | AJOUT_PANIER | COMMANDE | ANNULATION
    @Column(name = "type_interaction", nullable = false)
    private String typeInteraction;

    @Column(nullable = false)
    private Double points;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}