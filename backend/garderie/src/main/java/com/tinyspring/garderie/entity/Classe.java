package com.tinyspring.garderie.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import com.tinyspring.garderie.entity.Salle;

import java.io.Serializable;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "classe")
public class Classe implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom de la classe est obligatoire")
    @Column(nullable = false)
    private String nom;

    @NotBlank(message = "Le niveau de la classe est obligatoire")
    private String niveau;

    @NotNull(message = "La capacité max. est obligatoire")
    @Min(value = 1, message = "La capacité doit être d'au moins 1")
    private Integer capaciteMax;

    @NotNull(message = "L'affectation à une salle est obligatoire")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "salle_id")
    private Salle salle;

    @NotBlank(message = "L'année scolaire est requise (ex: 2025/2026)")
    private String anneeScolaire;

    @Min(value = 0, message = "L'âge minimum ne peut être négatif")
    private Integer ageMinimum;

    @Min(value = 1, message = "L'âge maximum doit être superieur à 0")
    private Integer ageMaximum;

    @Transient
    private Integer countEnfantsActifs = 0;
}
