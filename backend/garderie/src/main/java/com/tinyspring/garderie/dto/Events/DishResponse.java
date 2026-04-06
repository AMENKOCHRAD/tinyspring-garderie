package com.tinyspring.garderie.dto.Events;

import com.tinyspring.garderie.entity.Events.MealType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DishResponse {
    private Long id;
    private Long dailyMenuId;
    private MealType mealType;
    private String name;
    private String description;
    private String photoUrl;
    private String allergens;
    private String allergenConflictFlags;
}
