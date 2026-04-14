package com.tinyspring.garderie.entity.RH;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tinyspring.garderie.entity.RH.enums.StatutAnimatrice;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "animatrices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Animatrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String email;

    private String telephone;

    @Column(name = "date_embauche")
    private LocalDate dateEmbauche;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutAnimatrice statut;

    private String specialite;

    @Column(name = "photo_url")
    private String photoUrl;

    // ✅ JsonIgnore pour éviter la boucle infinie JSON
    @JsonIgnore
    @OneToMany(mappedBy = "animatrice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AbsenceConge> absenceConges;

    // ✅ JsonIgnore pour éviter la boucle infinie JSON
    @JsonIgnore
    @ManyToMany(mappedBy = "animatrices", fetch = FetchType.LAZY)
    private List<Formation> formations;
}