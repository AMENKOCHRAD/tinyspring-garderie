package com.tinyspring.garderie.entity.RH;

import com.tinyspring.garderie.entity.RH.enums.StatutAbsenceConge;
import com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "absence_conges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbsenceConge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animatrice_id", nullable = false)
    private Animatrice animatrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeAbsenceConge type;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutAbsenceConge statut;

    @Column(name = "nb_jours")
    private Integer nbJours;

    // ✅ Boolean objet (nullable) au lieu de boolean primitif
    @Column(name = "decision_automatique", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean decisionAutomatique = false;

    // ✅ Motif de la décision automatique
    @Column(name = "motif_decision", columnDefinition = "TEXT")
    private String motifDecision;
}