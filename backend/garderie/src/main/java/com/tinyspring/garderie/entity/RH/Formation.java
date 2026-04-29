package com.tinyspring.garderie.entity.RH;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tinyspring.garderie.entity.RH.enums.StatutFormation;
import com.tinyspring.garderie.entity.RH.enums.StatutInscription;
import com.tinyspring.garderie.entity.RH.enums.TypeFormation;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "formations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Formation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeFormation type;

    @Column(nullable = false)
    private String formateur;

    @Column(name = "lieu")
    private String lieu;

    @Column(name = "date_formation")
    private LocalDate dateFormation;

    @Column(name = "heure_debut")
    private LocalTime heureDebut;

    @Column(name = "heure_fin")
    private LocalTime heureFin;

    @Column(name = "places_max")
    private Integer placesMax;

    @Column(name = "duree_validite_mois")
    private Integer dureeValiditeMois;

    @Column(name = "obligatoire")
    private Boolean obligatoire = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutFormation statut = StatutFormation.OUVERTE;

    @JsonIgnoreProperties({"formation", "hibernateLazyInitializer", "handler"})
    @OneToMany(mappedBy = "formation", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<AnimatriceFormation> inscriptions;

    // ✅ Compte uniquement les INSCRITE (pas ABANDONNEE, LISTE_ATTENTE)
    public int getNbInscrits() {
        if (inscriptions == null) return 0;
        return (int) inscriptions.stream()
                .filter(i -> StatutInscription.INSCRITE.equals(i.getStatut()))
                .count();
    }

    // ✅ Places disponibles basées sur inscrits actifs uniquement
    public int getPlacesDisponibles() {
        if (placesMax == null) return 999;
        return Math.max(0, placesMax - getNbInscrits());
    }

    public boolean isComplet() {
        return placesMax != null && getNbInscrits() >= placesMax;
    }
}