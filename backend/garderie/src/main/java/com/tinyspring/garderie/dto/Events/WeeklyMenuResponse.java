package com.tinyspring.garderie.dto.Events;

import com.tinyspring.garderie.entity.events.MenuStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyMenuResponse {
    private Long id;
    private String title;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private MenuStatus status;
    private Boolean isTemplate;
    private String templateName;
    private LocalDateTime createdAt;

    @Builder.Default
    private List<DailyMenuResponse> dailyMenus = new ArrayList<>();
}