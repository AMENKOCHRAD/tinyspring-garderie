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

    @NotNull(message = "L'identifiant du menu journalier est obligatoire")
    private Long dailyMenuId;

    @NotNull(message = "Le type de plat est obligatoire")
    private MealType mealType;

    @NotNull(message = "Le nom du plat est obligatoire")
    @Size(max = 255, message = "Le nom du plat ne doit pas dépasser 255 caractères")
    private String name;

    @Size(max = 5000, message = "La description est trop longue")
    private String description;

    @Size(max = 500, message = "L'URL de la photo ne doit pas dépasser 500 caractères")
    private String photoUrl;

    @Size(max = 5000, message = "La liste des allergènes est trop longue")
    private String allergens;
}
