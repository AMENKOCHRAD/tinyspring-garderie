package com.tinyspring.garderie.repository.Events;

import com.tinyspring.garderie.entity.Events.AlternativeDish;
import com.tinyspring.garderie.entity.Events.MealType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlternativeDishRepository extends JpaRepository<AlternativeDish, Long>{
    List<AlternativeDish> findByMealTypeAndOriginalAllergenOrderByPriorityAsc(
            MealType mealType,
            String originalAllergen
    );
    List<AlternativeDish> findByOriginalAllergenIgnoreCaseAndMealTypeOrderByPriorityAsc(
            String originalAllergen,
            MealType mealType
    );

    Optional<AlternativeDish> findByNameIgnoreCaseAndOriginalAllergenIgnoreCaseAndMealType(
            String name,
            String originalAllergen,
            MealType mealType
    );
}
