package com.tinyspring.garderie.dto.Events;
import com.tinyspring.garderie.entity.Events.MenuStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyMenuRequest {
    @Size(max = 255, message = "Le titre ne doit pas dépasser 255 caractères")
    private String title;

    @NotNull(message = "La date de début de semaine est obligatoire")
    private LocalDate weekStartDate;

    @NotNull(message = "La date de fin de semaine est obligatoire")
    private LocalDate weekEndDate;

    private MenuStatus status;

    @NotNull(message = "Le champ isTemplate est obligatoire")
    private Boolean isTemplate;

    @Size(max = 255, message = "Le nom du template ne doit pas dépasser 255 caractères")
    private String templateName;

    @NotNull(message = "Le créateur est obligatoire")
    private Long createdBy;
}
