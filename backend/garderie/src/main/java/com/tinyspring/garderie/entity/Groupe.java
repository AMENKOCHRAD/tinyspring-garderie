package com.tinyspring.garderie.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import com.tinyspring.garderie.entity.Classe;

import java.io.Serializable;
import java.time.LocalTime;

@Entity
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

    @NotNull(message = "La capacité est obligatoire")
    @Min(value = 1, message = "La capacité doit être d'au moins 1")
    private Integer capacite;

    @NotNull(message = "Un animatrice (ID) est requise")
    @Column(nullable = false)
    private Long animatriceId;

    @NotNull(message = "L'affectation à une classe est obligatoire")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classe_id", nullable = false)
    private Classe classe;

    @NotNull(message = "L'horaire de début est requis")
    private LocalTime horaireDebut;

    @NotNull(message = "L'horaire de fin est requis")
    private LocalTime horaireFin;

    @NotBlank(message = "La langue principale est requise")
    private String languePrincipale;
}
