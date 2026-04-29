package com.tinyspring.garderie.service.events;

import com.tinyspring.garderie.dto.Events.DishRequest;
import com.tinyspring.garderie.entity.events.AlternativeDish;
import com.tinyspring.garderie.entity.events.MealType;
import com.tinyspring.garderie.repository.events.AlternativeDishRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlternativeDishServiceImplTest {

    @Mock
    private AlternativeDishRepository repository;

    @InjectMocks
    private AlternativeDishServiceImpl service;

    // ✅ 1. cas normal → alternative trouvée
    @Test
    void shouldReturnSafeAlternative_whenValidAlternativeExists() {
        DishRequest original = DishRequest.builder()
                .mealType(MealType.DESSERT)
                .allergens("gluten")
                .build();

        AlternativeDish alt = AlternativeDish.builder()
                .name("Compote de pommes")
                .description("Dessert sans gluten")
                .allergens("")
                .originalAllergen("gluten")
                .mealType(MealType.DESSERT)
                .priority(1)
                .build();

        when(repository.findByOriginalAllergenIgnoreCaseAndMealTypeOrderByPriorityAsc(
                "gluten", MealType.DESSERT))
                .thenReturn(List.of(alt));

        DishRequest result = service.generateAlternativeFor(original);

        assertEquals("Compote de pommes", result.getName());
        assertEquals("PLAT_ADAPTE", result.getAllergenConflictFlags());
    }

    // ❌ 2. plat null
    @Test
    void shouldThrow_whenDishIsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> service.generateAlternativeFor(null));
    }

    // ❌ 3. mealType null
    @Test
    void shouldThrow_whenMealTypeIsNull() {
        DishRequest request = new DishRequest();
        request.setAllergens("gluten");

        assertThrows(IllegalArgumentException.class,
                () -> service.generateAlternativeFor(request));
    }

    // ❌ 4. aucun allergène
    @Test
    void shouldThrow_whenNoAllergen() {
        DishRequest request = DishRequest.builder()
                .mealType(MealType.PLAT_PRINCIPAL)
                .allergens("")
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> service.generateAlternativeFor(request));
    }

    // 🔥 5. fallback créé quand aucune alternative safe
    @Test
    void shouldCreateFallback_whenNoSafeAlternativeExists() {
        DishRequest original = DishRequest.builder()
                .mealType(MealType.PLAT_PRINCIPAL)
                .allergens("gluten")
                .build();

        // alternative NON safe (contient gluten)
        AlternativeDish badAlt = AlternativeDish.builder()
                .name("Pain")
                .description("avec gluten")
                .allergens("gluten")
                .originalAllergen("gluten")
                .mealType(MealType.PLAT_PRINCIPAL)
                .priority(1)
                .build();

        when(repository.findByOriginalAllergenIgnoreCaseAndMealTypeOrderByPriorityAsc(
                "gluten", MealType.PLAT_PRINCIPAL))
                .thenReturn(List.of(badAlt));

        when(repository.findByNameIgnoreCaseAndOriginalAllergenIgnoreCaseAndMealType(
                anyString(), anyString(), any()))
                .thenReturn(Optional.empty());

        when(repository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DishRequest result = service.generateAlternativeFor(original);

        assertEquals("Riz aux légumes doux", result.getName());
    }

    // 🔍 6. vérifier filtrage allergène texte
    @Test
    void shouldRejectAlternative_whenContainsForbiddenKeyword() {
        DishRequest original = DishRequest.builder()
                .mealType(MealType.DESSERT)
                .allergens("lait")
                .build();

        AlternativeDish alt = AlternativeDish.builder()
                .name("Gâteau au lait")
                .description("dessert")
                .allergens("")
                .originalAllergen("lait")
                .mealType(MealType.DESSERT)
                .priority(1)
                .build();

        when(repository.findByOriginalAllergenIgnoreCaseAndMealTypeOrderByPriorityAsc(
                "lait", MealType.DESSERT))
                .thenReturn(List.of(alt));

        when(repository.findByNameIgnoreCaseAndOriginalAllergenIgnoreCaseAndMealType(
                anyString(), anyString(), any()))
                .thenReturn(Optional.empty());

        when(repository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DishRequest result = service.generateAlternativeFor(original);

        assertNotEquals("Gâteau au lait", result.getName());
    }
}