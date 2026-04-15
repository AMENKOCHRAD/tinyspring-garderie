package com.tinyspring.garderie.dto.RH;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultatEvaluationDTO {

    // Décision finale : AUTO_APPROUVE, AUTO_REFUSE, TRANSMIS_ADMIN
    private String decision;

    // Règle qui a déclenché la décision
    private String regleDeclenchee;

    // Explication lisible pour l'animatrice
    private String explication;

    // Jours déjà utilisés cette année
    private int joursDejaUtilises;

    // Jours restants après cette demande
    private int joursRestants;

    // Quota max pour ce type
    private int quotaMax;
}