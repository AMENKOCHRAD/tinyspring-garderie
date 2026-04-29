package com.tinyspring.garderie.dto.Events;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyMenuRequest {

    // Optional for standalone daily endpoints
    private Long weeklyMenuId;

    private LocalDate menuDate;

    private DayOfWeek dayOfWeek;

    @NotNull(message = "Le champ isVisibleToParents est obligatoire")
    private Boolean isVisibleToParents;

    private LocalDateTime publishedAt;

    @Valid
    @Builder.Default
    private List<DishRequest> dishes = new ArrayList<>();
}