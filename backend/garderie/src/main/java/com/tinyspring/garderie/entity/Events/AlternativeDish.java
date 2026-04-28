package com.tinyspring.garderie.entity.Events;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "alternative_dishes")
public class AlternativeDish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MealType mealType;

    @Column(nullable = false)
    private String originalAllergen;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    private String allergens;

    @Column(nullable = false)
    private Integer priority;
}
