package com.tinyspring.garderie.dto.Events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyMenuResponse {
    private Long id;
    private Long weeklyMenuId;
    private LocalDate menuDate;
    private DayOfWeek dayOfWeek;
    private Boolean isVisibleToParents;
    private LocalDateTime publishedAt;
    private List<DishResponse> dishes;
}
