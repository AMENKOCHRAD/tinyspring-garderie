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
import java.time.LocalTime;

@Entity(name = "ClassesGroupe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "groupe")
public class Groupe implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom du groupe est obligatoire")
    @Column(nullable = false)
    private String nom;

    @NotNull(message = "La capacite est obligatoire")
    @Min(value = 1, message = "La capacite doit etre d'au moins 1")
    private Integer capacite;

    @NotNull(message = "Une animatrice (ID) est requise")
    @Column(nullable = false)
    private Long animatriceId;

    @NotNull(message = "L'affectation a une classe est obligatoire")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classe_id", nullable = false)
    private Classe classe;

    @NotNull(message = "L'horaire de debut est requis")
    private LocalTime horaireDebut;

    @NotNull(message = "L'horaire de fin est requis")
    private LocalTime horaireFin;

    @NotBlank(message = "La langue principale est requise")
    private String languePrincipale;
}
