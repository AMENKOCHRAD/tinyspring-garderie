package com.tinyspring.garderie.entity.Events;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_menu")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyMenu {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long weeklyMenuId;

    private LocalDate menuDate;

    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    private boolean isVisibleToParents;

    private LocalDateTime publishedAt;
}
