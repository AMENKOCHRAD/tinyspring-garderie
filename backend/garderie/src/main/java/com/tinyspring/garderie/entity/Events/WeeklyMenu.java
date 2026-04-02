package com.tinyspring.garderie.entity.Events;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weekly_menu")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private LocalDate weekStartDate;
    private LocalDate weekEndDate;

    @Enumerated(EnumType.STRING)
    private MenuStatus status;

    private boolean isTemplate;

    private String templateName;

    private Long createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
