package com.tinyspring.garderie.entity.RH;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tinyspring.garderie.entity.RH.enums.StatutInscription;
import com.tinyspring.garderie.entity.RH.enums.StatutValidite;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "animatrice_formations",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"animatrice_id", "formation_id"}
        ))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnimatriceFormation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ Expose animatrice sans ses absences ni ses formations (évite boucle)
    @JsonIgnoreProperties({"absenceConges", "inscriptions", "hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "animatrice_id", nullable = false)
    private Animatrice animatrice;

    // ✅ Expose formation sans ses inscriptions (évite boucle infinie)
    @JsonIgnoreProperties({"inscriptions", "hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "formation_id", nullable = false)
    private Formation formation;

    @Column(name = "date_inscription", nullable = false)
    private LocalDate dateInscription;

    @Column(name = "date_completion")
    private LocalDate dateCompletion;

    @Column(name = "date_expiration")
    private LocalDate dateExpiration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutInscription statut = StatutInscription.INSCRITE;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_validite")
    private StatutValidite statutValidite = StatutValidite.VALIDE;

    @Column(name = "certification_generee")
    private Boolean certificationGeneree = false;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @PrePersist
    @PreUpdate
    public void calculerStatutValidite() {
        if (dateExpiration == null) {
            this.statutValidite = StatutValidite.VALIDE;
            return;
        }
        LocalDate aujourd_hui = LocalDate.now();
        if (aujourd_hui.isAfter(dateExpiration)) {
            this.statutValidite = StatutValidite.EXPIREE;
        } else if (aujourd_hui.isAfter(dateExpiration.minusMonths(3))) {
            this.statutValidite = StatutValidite.BIENTOT_EXPIREE;
        } else {
            this.statutValidite = StatutValidite.VALIDE;
        }
    }
}