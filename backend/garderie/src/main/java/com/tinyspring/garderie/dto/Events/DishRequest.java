package com.tinyspring.garderie.dto.Events;

import com.tinyspring.garderie.entity.Events.MealType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DishRequest {

    private Long dailyMenuId;

    @NotNull(message = "Le type de plat est obligatoire")
    private MealType mealType;

    @NotNull(message = "Le nom du plat est obligatoire")
    @Size(max = 255, message = "Le nom du plat ne doit pas depasser 255 caracteres")
    private String name;

    @Size(max = 5000, message = "La description est trop longue")
    private String description;

    @Size(max = 5000, message = "La liste des allergenes est trop longue")
    private String allergens;

    @Size(max = 5000, message = "Les flags d'allergenes sont trop longs")
    private String allergenConflictFlags;
}
