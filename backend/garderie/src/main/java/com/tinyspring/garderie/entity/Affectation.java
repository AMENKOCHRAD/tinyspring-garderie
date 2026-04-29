package com.tinyspring.garderie.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import com.tinyspring.garderie.entity.Groupe;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "affectation")
public class Affectation implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Le groupe est obligatoire")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "groupe_id", nullable = false)
    private Groupe groupe;

    @NotNull(message = "L'ID enfant est obligatoire")
    @Column(nullable = false)
    private Long enfantId;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate dateFin;

    @NotBlank(message = "Le statut est obligatoire (ex: ACTIF, SUSPENDU, TERMINE)")
    private String statut;
}
