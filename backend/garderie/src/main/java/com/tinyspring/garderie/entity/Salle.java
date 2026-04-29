package com.tinyspring.garderie.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.io.Serializable;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "salle")
public class Salle implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom de la salle est obligatoire")
    @Column(nullable = false)
    private String nom;

    @Min(value = 0, message = "La surface doit être positive")
    private Double surface;

    @NotBlank(message = "Le type de la salle est obligatoire")
    private String type; // Dortoir, Salle de jeu, Cantine...

    private Boolean climatise;

    private String equipements;

    private Boolean disponible;
}
