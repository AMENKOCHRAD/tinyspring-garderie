package com.tinyspring.garderie.entity.RH;

import com.tinyspring.garderie.entity.RH.enums.StatutFormation;
import com.tinyspring.garderie.entity.RH.enums.TypeFormation;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
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

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    private String formateur;

    @Column(name = "places_max")
    private Integer placesMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_inscription")
    private StatutFormation statutInscription;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "formation_animatrice",
            joinColumns = @JoinColumn(name = "formation_id"),
            inverseJoinColumns = @JoinColumn(name = "animatrice_id")
    )
    private List<Animatrice> animatrices;
}