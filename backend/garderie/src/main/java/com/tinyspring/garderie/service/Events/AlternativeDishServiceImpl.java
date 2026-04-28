package com.tinyspring.garderie.service.Events;

import com.tinyspring.garderie.dto.Events.DishRequest;
import com.tinyspring.garderie.entity.Events.MealType;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AlternativeDishServiceImpl implements AlternativeDishService {

    private record AlternativeOption(
            MealType mealType,
            String name,
            String description
    ) {}

    private static final Map<MealType, List<AlternativeOption>> SAFE_OPTIONS = Map.of(
            MealType.ENTREE, List.of(
                    new AlternativeOption(MealType.ENTREE, "Soupe de légumes doux", "Entrée légère aux légumes cuits, sans allergènes majeurs."),
                    new AlternativeOption(MealType.ENTREE, "Salade de carottes cuites", "Carottes douces assaisonnées simplement.")
            ),
            MealType.PLAT_PRINCIPAL, List.of(
                    new AlternativeOption(MealType.PLAT_PRINCIPAL, "Riz aux légumes doux", "Riz tendre accompagné de légumes cuits."),
                    new AlternativeOption(MealType.PLAT_PRINCIPAL, "Pommes de terre vapeur aux légumes", "Pommes de terre douces avec légumes cuits."),
                    new AlternativeOption(MealType.PLAT_PRINCIPAL, "Poulet aux légumes et riz", "Poulet tendre avec riz et légumes doux.")
            ),
            MealType.DESSERT, List.of(
                    new AlternativeOption(MealType.DESSERT, "Compote de pommes", "Dessert doux à base de pommes cuites."),
                    new AlternativeOption(MealType.DESSERT, "Fruit frais", "Fruit de saison coupé.")
            ),
            MealType.GOUTER, List.of(
                    new AlternativeOption(MealType.GOUTER, "Fruits frais coupés", "Goûter naturel avec fruits de saison."),
                    new AlternativeOption(MealType.GOUTER, "Compote maison", "Goûter doux à base de fruits.")
            )
    );

    @Override
    public DishRequest generateAlternativeFor(DishRequest originalDish) {
        if (originalDish == null || originalDish.getMealType() == null) {
            throw new IllegalArgumentException("Plat original invalide");
        }

        Set<String> forbiddenAllergens = normalizeToSet(originalDish.getAllergens());

        if (forbiddenAllergens.isEmpty()) {
            throw new IllegalArgumentException("Le plat original ne contient aucun allergène");
        }

        List<AlternativeOption> options = SAFE_OPTIONS.getOrDefault(
                originalDish.getMealType(),
                List.of()
        );

        for (AlternativeOption option : options) {
            if (isSafe(option, forbiddenAllergens)) {
                return DishRequest.builder()
                        .mealType(originalDish.getMealType())
                        .name(option.name())
                        .description(option.description())
                        .allergens("")
                        .allergenConflictFlags("PLAT_ADAPTE")
                        .build();
            }
        }

        throw new IllegalStateException(
                "Aucune alternative sûre trouvée pour le plat : " + originalDish.getName()
        );
    }

    private boolean isSafe(AlternativeOption option, Set<String> forbiddenAllergens) {
        String text = normalize(option.name() + " " + option.description());

        for (String allergen : forbiddenAllergens) {
            if (containsForbiddenKeyword(text, allergen)) {
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
            return Set.of();
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
}