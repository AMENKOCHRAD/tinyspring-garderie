package com.tinyspring.garderie.repository.events;

import com.tinyspring.garderie.entity.events.AlternativeDish;
import com.tinyspring.garderie.entity.events.MealType;
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
