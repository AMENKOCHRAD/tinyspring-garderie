package com.tinyspring.garderie.entity.boutique;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "predictions")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "produit_id", nullable = false)
    private Long produitId;

    @Column(name = "produit_nom")
    private String produitNom;

    @Column(name = "stock_actuel")
    private Integer stockActuel;

    @Column(name = "total_prevu_4_semaines")
    private Integer totalPrevu4Semaines;

    @Column(name = "alerte_rupture")
    private Boolean alerteRupture;

    @Column(name = "date_calcul")
    private LocalDateTime dateCalcul;

    // JSON des prédictions semaine par semaine
    @Column(name = "details_json", length = 5000)
    private String detailsJson;
}