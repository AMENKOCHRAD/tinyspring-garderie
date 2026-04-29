package com.tinyspring.garderie.entity.Classes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @NotNull(message = "La capacite max. est obligatoire")
    @Min(value = 1, message = "La capacite doit etre d'au moins 1")
    private Integer capaciteMax;

    @NotNull(message = "L'affectation a une salle est obligatoire")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "salle_id")
    private Salle salle;

    @NotBlank(message = "L'annee scolaire est requise")
    private String anneeScolaire;

    @Min(value = 0, message = "L'age minimum ne peut etre negatif")
    private Integer ageMinimum;

    @Min(value = 1, message = "L'age maximum doit etre superieur a 0")
    private Integer ageMaximum;
}
