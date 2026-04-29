package com.tinyspring.garderie.entity.RH;

import com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "quota_conges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotaConge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Type d'absence concerné
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private TypeAbsenceConge type;

    // Nombre de jours max autorisés par an
    @Column(name = "nb_jours_max", nullable = false)
    private Integer nbJoursMax;

    // Délai minimum de prévenance en jours
    @Column(name = "delai_prevenance_jours", nullable = false)
    private Integer delaiPrevenanceJours;

    // Nombre minimum d'animatrices présentes requis
    @Column(name = "effectif_minimum", nullable = false)
    private Integer effectifMinimum;

    // Approbation automatique activée
    @Column(name = "auto_approbation", nullable = false)
    private boolean autoApprobation;
}