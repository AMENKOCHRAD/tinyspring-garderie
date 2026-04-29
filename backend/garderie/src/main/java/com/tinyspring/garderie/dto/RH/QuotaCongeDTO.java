package com.tinyspring.garderie.dto.RH;

import com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotaCongeDTO {
    private Long id;
    private TypeAbsenceConge type;
    private Integer nbJoursMax;
    private Integer delaiPrevenanceJours;
    private Integer effectifMinimum;
    private boolean autoApprobation;

    // Infos calculées (non stockées en DB)
    private int joursUtilisesAnneeEnCours;
    private int joursRestants;
}