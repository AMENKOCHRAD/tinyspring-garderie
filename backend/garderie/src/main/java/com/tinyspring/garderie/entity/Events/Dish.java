package com.tinyspring.garderie.entity.Events;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dish")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Dish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long dailyMenuId;

    @Enumerated(EnumType.STRING)
    private MealType mealType;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String photoUrl;

    @Column(columnDefinition = "TEXT")
    private String allergens;

    @Column(columnDefinition = "TEXT")
    private String allergenConflictFlags;
}
