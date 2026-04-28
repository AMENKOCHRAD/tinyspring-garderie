package com.tinyspring.garderie.service.Events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyspring.garderie.dto.Events.DailyMenuRequest;
import com.tinyspring.garderie.dto.Events.DishRequest;
import com.tinyspring.garderie.dto.Events.WeeklyMenuAiGenerateRequest;
import com.tinyspring.garderie.dto.Events.WeeklyMenuRequest;
import com.tinyspring.garderie.entity.Events.MealType;
import com.tinyspring.garderie.entity.Events.MenuStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MenuAiService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final AlternativeDishService alternativeDishService;

    private static final Set<String> ALLOWED_ALLERGENS = Set.of(
            "gluten", "lait", "oeufs", "poisson", "arachides",
            "soja", "noix", "céleri", "moutarde", "sésame", "sulfites"
    );

    public WeeklyMenuRequest generateWeeklyMenuDraft(WeeklyMenuAiGenerateRequest request) {
        try {
            LocalDate weekStart = LocalDate.parse(request.getWeekStartDate());

            String prompt = buildPrompt(weekStart);

            ResponseEntity<Map<String, Object>> response = callOllama(prompt, 3500);

            if (response.getBody() == null) {
                throw new IllegalStateException("Réponse Ollama null");
            }

            Object rawResponse = response.getBody().get("response");

            if (rawResponse == null) {
                throw new IllegalStateException("Réponse IA vide");
            }

            return parseAiResponse(rawResponse.toString(), weekStart);

        } catch (Exception e) {
            throw new IllegalStateException("Erreur pendant la génération IA : " + e.getMessage(), e);
        }
    }

    private ResponseEntity<Map<String, Object>> callOllama(String prompt, int numPredict) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "model", "llama3.2:3b",
                "prompt", prompt,
                "stream", false,
                "format", "json",
                "keep_alive", "10m",
                "options", Map.of(
                        "temperature", 0.2,
                        "num_predict", numPredict,
                        "num_ctx", 8192
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        @SuppressWarnings("unchecked")
        Class<Map<String, Object>> responseType = (Class<Map<String, Object>>) (Class<?>) Map.class;

        return restTemplate.exchange(
                "http://localhost:11434/api/generate",
                HttpMethod.POST,
                entity,
                responseType
        );
    }

    private String buildPrompt(LocalDate weekStart) {
        return """
        Réponds uniquement avec un JSON valide. Aucun texte avant ou après.

        Génère uniquement le menu principal d'une garderie tunisienne pour enfants 2-6 ans.
        Ne génère PAS les alternatives : le backend les ajoute automatiquement.

        Menu doux, simple, équilibré, non épicé.

        Dates obligatoires :
        MONDAY=%s
        TUESDAY=%s
        WEDNESDAY=%s
        THURSDAY=%s
        FRIDAY=%s

        Format exact :
        {
          "title": "Menu semaine du %s",
          "dailyMenus": [
            {
              "menuDate": "YYYY-MM-DD",
              "dayOfWeek": "MONDAY",
              "isVisibleToParents": true,
              "dishes": [
                {
                  "mealType": "ENTREE",
                  "name": "string",
                  "description": "string",
                  "allergens": ""
                }
              ]
            }
          ]
        }

        Contraintes :
        - dailyMenus contient exactement 5 jours.
        - Chaque jour contient exactement 4 plats :
          1 ENTREE, 1 PLAT_PRINCIPAL, 1 DESSERT, 1 GOUTER.
        - mealType autorisé uniquement : ENTREE, PLAT_PRINCIPAL, DESSERT, GOUTER.
        - allergens autorisés uniquement : gluten, lait, oeufs, poisson, arachides, soja, noix, céleri, moutarde, sésame, sulfites.
        - Chaque jour doit contenir au moins 2 plats avec allergènes.
        - Chaque jour doit contenir au moins 1 plat avec gluten.
        - Chaque jour doit contenir au moins 1 plat avec lait ou oeufs ou poisson.

        Règles allergènes :
        - pain, couscous, pâtes, semoule, biscuit, cake, gâteau, tarte => gluten
        - lait, yaourt, fromage, beurre, crème => lait
        - oeuf, omelette, cake, gâteau => oeufs
        - poisson, thon, sardine, saumon => poisson
        - cacahuète, arachide => arachides
        - noix, amande, noisette => noix

        Interdictions :
        - ne jamais mettre poulet avec allergène poisson
        - ne jamais mettre riz simple avec gluten
        - ne jamais mettre compote avec lait
        - éviter harissa, merguez, fritures lourdes, plats épicés, plats sophistiqués.

        JSON complet et fermé.
        """.formatted(
                weekStart,
                weekStart.plusDays(1),
                weekStart.plusDays(2),
                weekStart.plusDays(3),
                weekStart.plusDays(4),
                weekStart
        );
    }

    private WeeklyMenuRequest parseAiResponse(String rawJson, LocalDate weekStart) {
        try {
            String cleaned = rawJson
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            JsonNode root = objectMapper.readTree(cleaned);

            WeeklyMenuRequest request = WeeklyMenuRequest.builder()
                    .title(root.path("title").asText("Menu généré avec IA"))
                    .weekStartDate(weekStart)
                    .weekEndDate(weekStart.plusDays(6))
                    .status(MenuStatus.DRAFT)
                    .isTemplate(false)
                    .templateName(null)
                    .dailyMenus(new ArrayList<>())
                    .build();

            JsonNode dailyMenusNode = root.path("dailyMenus");

            if (!dailyMenusNode.isArray()) {
                throw new IllegalStateException("dailyMenus absent ou invalide");
            }

            DayOfWeek[] orderedDays = {
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY
            };

            for (int i = 0; i < Math.min(dailyMenusNode.size(), 5); i++) {
                JsonNode dayNode = dailyMenusNode.get(i);

                DailyMenuRequest dailyMenu = DailyMenuRequest.builder()
                        .menuDate(weekStart.plusDays(i))
                        .dayOfWeek(orderedDays[i])
                        .isVisibleToParents(dayNode.path("isVisibleToParents").asBoolean(true))
                        .dishes(new ArrayList<>())
                        .build();

                JsonNode dishesNode = dayNode.path("dishes");

                if (!dishesNode.isArray()) {
                    throw new IllegalStateException("dishes absent ou invalide");
                }

                for (JsonNode dishNode : dishesNode) {
                    String dishName = dishNode.path("name").asText("").trim();
                    String description = dishNode.path("description").asText("").trim();

                    String allergens = normalizeAllergens(
                            dishNode.path("allergens").asText(""),
                            dishName,
                            description
                    );

                    DishRequest dish = DishRequest.builder()
                            .mealType(safeMealType(dishNode.path("mealType").asText()))
                            .name(dishName)
                            .description(description)
                            .allergens(allergens)
                            .allergenConflictFlags("")
                            .build();

                    dailyMenu.getDishes().add(dish);

                    if (!allergens.isBlank()) {
                        dailyMenu.getDishes().add(
                                alternativeDishService.generateAlternativeFor(dish)
                        );
                    }
                }

                request.getDailyMenus().add(dailyMenu);
            }

            if (request.getDailyMenus().size() != 5) {
                throw new IllegalStateException("Le menu généré doit contenir exactement 5 jours");
            }

            return request;

        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'interpréter la réponse IA : " + e.getMessage(), e);
        }
    }

    private MealType safeMealType(String value) {
        if (value == null || value.isBlank()) {
            return MealType.ENTREE;
        }

        String normalized = normalize(value)
                .toUpperCase()
                .replace("PLAT PRINCIPAL", "PLAT_PRINCIPAL")
                .replace("PLAT PRINCIPALE", "PLAT_PRINCIPAL")
                .replace("ENTREE", "ENTREE")
                .replace("GOUTER", "GOUTER");

        try {
            return MealType.valueOf(normalized);
        } catch (Exception e) {
            return MealType.ENTREE;
        }
    }

    private String normalizeAllergens(String raw, String dishName, String description) {
        Set<String> allergens = new LinkedHashSet<>();

        if (raw != null && !raw.isBlank()) {
            Arrays.stream(raw.split("[,;\\n]"))
                    .map(String::trim)
                    .map(this::normalize)
                    .filter(ALLOWED_ALLERGENS::contains)
                    .forEach(allergens::add);
        }

        String text = normalize(
                (dishName == null ? "" : dishName) + " " + (description == null ? "" : description)
        );

        if (containsAny(text, "pain", "couscous", "pates", "semoule", "biscuit", "biscuits", "cake", "gateau", "tarte")) {
            allergens.add("gluten");
        }

        if (containsAny(text, "lait", "yaourt", "fromage", "beurre", "creme")) {
            allergens.add("lait");
        }

        if (containsAny(text, "oeuf", "oeufs", "omelette", "cake", "gateau")) {
            allergens.add("oeufs");
        }

        if (containsAny(text, "poisson", "thon", "sardine", "saumon")) {
            allergens.add("poisson");
        }

        if (containsAny(text, "cacahuete", "arachide", "arachides")) {
            allergens.add("arachides");
        }

        if (containsAny(text, "noix", "amande", "amandes", "noisette", "noisettes")) {
            allergens.add("noix");
        }

        if (containsAny(text, "celeri")) {
            allergens.add("céleri");
        }

        if (containsAny(text, "moutarde")) {
            allergens.add("moutarde");
        }

        if (containsAny(text, "sesame")) {
            allergens.add("sésame");
        }

        if (containsAny(text, "soja")) {
            allergens.add("soja");
        }

        if (containsAny(text, "sulfite", "sulfites")) {
            allergens.add("sulfites");
        }

        return String.join(", ", allergens);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(value.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}