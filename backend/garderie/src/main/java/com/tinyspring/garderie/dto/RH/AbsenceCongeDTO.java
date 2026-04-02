package com.tinyspring.garderie.dto.RH;

import com.tinyspring.garderie.entity.RH.enums.StatutAbsenceConge;
import com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbsenceCongeDTO {

    private Long id;

    @NotNull(message = "L'animatrice est obligatoire")
    private Long animatriceId;

    private String animatriceNom;
    private String animatricePrenom;

    @NotNull(message = "Le type est obligatoire")
    private TypeAbsenceConge type;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate dateFin;

    private String motif;

    private StatutAbsenceConge statut;

    private Integer nbJours;
}