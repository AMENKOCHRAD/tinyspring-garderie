package com.tinyspring.garderie.dto.Events;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyMenuRequest {
    @NotNull(message = "L'identifiant du menu hebdomadaire est obligatoire")
    private Long weeklyMenuId;

    @NotNull(message = "La date du menu est obligatoire")
    private LocalDate menuDate;

    @NotNull(message = "Le jour de la semaine est obligatoire")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Le champ isVisibleToParents est obligatoire")
    private Boolean isVisibleToParents;

    private LocalDateTime publishedAt;
}
