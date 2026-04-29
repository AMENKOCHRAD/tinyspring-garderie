package com.tinyspring.garderie.service.events;

import com.tinyspring.garderie.dto.Events.DishRequest;
import com.tinyspring.garderie.entity.events.AlternativeDish;
import com.tinyspring.garderie.entity.events.MealType;
import com.tinyspring.garderie.repository.events.AlternativeDishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlternativeDishServiceImpl implements AlternativeDishService {

    private final AlternativeDishRepository alternativeDishRepository;

    @Override
    public DishRequest generateAlternativeFor(DishRequest originalDish) {
        if (originalDish == null || originalDish.getMealType() == null) {
            throw new IllegalArgumentException("Plat original invalide");
        }

        Set<String> forbiddenAllergens = normalizeToSet(originalDish.getAllergens());

        if (forbiddenAllergens.isEmpty()) {
            throw new IllegalArgumentException("Le plat original ne contient aucun allergène");
        }

        for (String allergen : forbiddenAllergens) {
            List<AlternativeDish> alternatives =
                    alternativeDishRepository.findByOriginalAllergenIgnoreCaseAndMealTypeOrderByPriorityAsc(
                            allergen,
                            originalDish.getMealType()
                    );

            for (AlternativeDish alternative : alternatives) {
                if (isSafe(alternative, forbiddenAllergens)) {
                    return toDishRequest(alternative, originalDish.getMealType());
                }
            }
        }

        AlternativeDish fallback = createAndSaveFallbackAlternative(originalDish, forbiddenAllergens);

        return toDishRequest(fallback, originalDish.getMealType());
    }

    private DishRequest toDishRequest(AlternativeDish alternative, MealType mealType) {
        return DishRequest.builder()
                .mealType(mealType)
                .name(alternative.getName())
                .description(alternative.getDescription())
                .allergens(alternative.getAllergens() == null ? "" : alternative.getAllergens())
                .allergenConflictFlags("PLAT_ADAPTE")
                .build();
    }

    private AlternativeDish createAndSaveFallbackAlternative(
            DishRequest originalDish,
            Set<String> forbiddenAllergens
    ) {
        String firstAllergen = forbiddenAllergens.iterator().next();

        AlternativeData data = getFallbackData(originalDish.getMealType(), firstAllergen);

        return alternativeDishRepository
                .findByNameIgnoreCaseAndOriginalAllergenIgnoreCaseAndMealType(
                        data.name(),
                        firstAllergen,
                        originalDish.getMealType()
                )
                .orElseGet(() -> alternativeDishRepository.save(
                        AlternativeDish.builder()
                                .name(data.name())
                                .description(data.description())
                                .allergens("")
                                .originalAllergen(firstAllergen)
                                .mealType(originalDish.getMealType())
                                .priority(99)
                                .build()
                ));
    }

    private AlternativeData getFallbackData(MealType mealType, String allergen) {
        return switch (mealType) {
            case ENTREE -> new AlternativeData(
                    "Soupe de légumes doux",
                    "Entrée légère aux légumes cuits, alternative générée automatiquement sans " + allergen + "."
            );
            case PLAT_PRINCIPAL -> new AlternativeData(
                    "Riz aux légumes doux",
                    "Riz tendre accompagné de légumes cuits, alternative générée automatiquement sans " + allergen + "."
            );
            case DESSERT -> new AlternativeData(
                    "Compote de pommes",
                    "Dessert doux à base de pommes cuites, alternative générée automatiquement sans " + allergen + "."
            );
            case GOUTER -> new AlternativeData(
                    "Fruits frais coupés",
                    "Goûter naturel avec fruits de saison, alternative générée automatiquement sans " + allergen + "."
            );
        };
    }

    private boolean isSafe(AlternativeDish alternative, Set<String> forbiddenAllergens) {
        Set<String> alternativeAllergens = normalizeToSet(alternative.getAllergens());

        for (String allergen : forbiddenAllergens) {
            String normalizedAllergen = normalize(allergen);

            if (alternativeAllergens.contains(normalizedAllergen)) {
                return false;
            }

            String text = normalize(
                    (alternative.getName() == null ? "" : alternative.getName()) + " " +
                            (alternative.getDescription() == null ? "" : alternative.getDescription())
            );

            if (containsForbiddenKeyword(text, normalizedAllergen)) {
                return false;
            }
        }

        return true;
    }

    private boolean containsForbiddenKeyword(String text, String allergen) {
        return switch (allergen) {
            case "gluten" -> containsAny(text, "pain", "couscous", "pates", "pate", "semoule", "biscuit", "biscuits", "cake", "gateau", "tarte");
            case "lait" -> containsAny(text, "lait", "yaourt", "fromage", "beurre", "creme");
            case "oeufs" -> containsAny(text, "oeuf", "oeufs", "omelette", "cake", "gateau");
            case "poisson" -> containsAny(text, "poisson", "thon", "sardine", "saumon");
            case "arachides" -> containsAny(text, "cacahuete", "arachide", "arachides");
            case "noix" -> containsAny(text, "noix", "amande", "amandes", "noisette", "noisettes");
            case "celeri", "céleri" -> containsAny(text, "celeri");
            case "moutarde" -> containsAny(text, "moutarde");
            case "sesame", "sésame" -> containsAny(text, "sesame");
            case "soja" -> containsAny(text, "soja");
            case "sulfites" -> containsAny(text, "sulfite", "sulfites");
            default -> false;
        };
    }

    private Set<String> normalizeToSet(String value) {
        if (value == null || value.isBlank()) {
            return new LinkedHashSet<>();
        }

        return Arrays.stream(value.split("[,;\\n]"))
                .map(String::trim)
                .map(this::normalize)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(value.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private record AlternativeData(String name, String description) {
    }
}