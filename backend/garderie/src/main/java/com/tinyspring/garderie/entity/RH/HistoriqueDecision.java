package com.tinyspring.garderie.entity.RH;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historique_decisions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoriqueDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // La demande d'absence concernée
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "absence_id", nullable = false)
    private AbsenceConge absenceConge;

    // Règle qui a déclenché la décision
    @Column(name = "regle_declenchee", nullable = false)
    private String regleDeclenchee;

    // Décision prise automatiquement
    @Column(nullable = false)
    private String decision; // AUTO_APPROUVE, AUTO_REFUSE, TRANSMIS_ADMIN

    // Explication détaillée de la décision
    @Column(nullable = false, columnDefinition = "TEXT")
    private String explication;

    // Date et heure de la décision
    @Column(name = "date_decision", nullable = false)
    private LocalDateTime dateDecision;

    @PrePersist
    public void prePersist() {
        this.dateDecision = LocalDateTime.now();
    }
}