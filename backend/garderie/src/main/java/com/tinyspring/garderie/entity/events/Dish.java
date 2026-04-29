package com.tinyspring.garderie.entity.events;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dish")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_menu_id", nullable = false)
    private DailyMenu dailyMenu;

    @Enumerated(EnumType.STRING)
    private MealType mealType;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String allergens;

    @Column(columnDefinition = "TEXT")
    private String allergenConflictFlags;
}
