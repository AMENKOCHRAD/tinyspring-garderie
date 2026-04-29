package com.tinyspring.garderie.dto.RH;

import com.tinyspring.garderie.entity.RH.enums.StatutFormation;
import com.tinyspring.garderie.entity.RH.enums.TypeFormation;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormationDTO {

    private Long id;

    @NotBlank(message = "Le titre est obligatoire")
    private String titre;

    private String description;

    @NotNull(message = "Le type est obligatoire")
    private TypeFormation type;

    private LocalDate dateDebut;
    private LocalDate dateFin;

    private String formateur;

    @Min(value = 1, message = "Le nombre de places doit être supérieur à 0")
    private Integer placesMax;

    private StatutFormation statutInscription;

    private List<AnimatriceDTO> animatrices;
}