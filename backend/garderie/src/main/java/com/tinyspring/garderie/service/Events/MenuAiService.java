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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MenuAiService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public WeeklyMenuRequest generateWeeklyMenuDraft(WeeklyMenuAiGenerateRequest request) {
        try {
            LocalDate weekStart = LocalDate.parse(request.getWeekStartDate());

            String prompt = buildPrompt(weekStart);
            System.out.println("=== PROMPT ENVOYE A OLLAMA ===");
            System.out.println(prompt);

            ResponseEntity<Map> response = callOllama(prompt, 3500);

            System.out.println("=== REPONSE BRUTE OLLAMA ===");
            System.out.println(response.getBody());

            if (response.getBody() == null) {
                throw new IllegalStateException("Réponse Ollama null");
            }

            Object rawResponse = response.getBody().get("response");
            if (rawResponse == null) {
                throw new IllegalStateException("Réponse IA vide.");
            }

            return parseAiResponse(rawResponse.toString(), weekStart);

        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Erreur pendant la génération IA : " + e.getMessage(), e);
        }
    }

    private ResponseEntity<Map> callOllama(String prompt, int numPredict) {
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

        return restTemplate.exchange(
                "http://localhost:11434/api/generate",
                HttpMethod.POST,
                entity,
                Map.class
        );
    }

    private String buildPrompt(LocalDate weekStart) {
        return """
        Réponds uniquement avec un JSON valide. Aucun texte avant ou après.

        Génère un menu de garderie tunisienne pour enfants 2-6 ans.
        Menu doux, simple, équilibré, non épicé.

        Dates obligatoires :
        MONDAY=%s
        TUESDAY=%s
        WEDNESDAY=%s
        THURSDAY=%s
        FRIDAY=%s

        Format :
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
        - Chaque jour contient exactement : 1 ENTREE, 1 PLAT_PRINCIPAL, 1 DESSERT, 1 GOUTER.
        - mealType autorisé : ENTREE, PLAT_PRINCIPAL, DESSERT, GOUTER.
        - allergens autorisés uniquement : gluten, lait, oeufs, poisson, arachides, soja, noix, céleri, moutarde, sésame, sulfites.
                - Chaque jour doit contenir obligatoirement AU MOINS 2 plats avec allergènes.
                
                - Pour garantir cela, utilise des plats naturels contenant allergènes :
                  yaourt, fromage, pain, couscous, pâtes, cake, omelette, poisson.
                
                - Règles obligatoires :
                  - pain, couscous, pâtes, semoule, biscuit, cake => allergens doit contenir "gluten"
                  - lait, yaourt, fromage, beurre, crème => allergens doit contenir "lait"
                  - oeuf, omelette, gâteau, cake => allergens doit contenir "oeufs"
                  - poisson, thon, sardine, saumon => allergens doit contenir "poisson"
                
                - Chaque jour doit contenir :
                  → au moins 1 plat avec "gluten"
                  → au moins 1 plat avec "lait" ou "oeufs" ou "poisson"
                
                - Interdictions :
                  - ne jamais mettre allergen incohérent
                  - exemple interdit : poulet avec poisson
                  - exemple interdit : riz avec gluten
        - Exemples cohérents :
          pain/couscous/pâtes/semoule = gluten
          yaourt/fromage/lait = lait
          poisson/thon/sardine = poisson
          omelette/gâteau = oeufs
        - Exemples interdits :
          poulet avec poisson
          riz simple avec gluten
          compote avec lait
        - Évite harissa, merguez, fritures lourdes, plats épicés, plats sophistiqués.
        - Varie les plats entre les jours.
        - JSON complet et fermé.
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

            System.out.println("=== JSON NETTOYE ===");
            System.out.println(cleaned);

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

                if (dishesNode.isArray()) {
                    for (JsonNode dishNode : dishesNode) {
                        String allergens = normalizeAllergens(dishNode.path("allergens").asText(""));

                        DishRequest dish = DishRequest.builder()
                                .mealType(safeMealType(dishNode.path("mealType").asText()))
                                .name(dishNode.path("name").asText(""))
                                .description(dishNode.path("description").asText(""))
                                .allergens(allergens)
                                .allergenConflictFlags("")
                                .build();

                        dailyMenu.getDishes().add(dish);

                        if (!allergens.isBlank()) {
                            DishRequest alternative = generateAlternativeDishOnly(dish);

                            if (alternative != null) {
                                dailyMenu.getDishes().add(alternative);
                            }
                        }
                    }
                }

                request.getDailyMenus().add(dailyMenu);
            }

            if (request.getDailyMenus().size() < 5) {
                System.out.println("⚠️ Menu incomplet généré par IA : " + request.getDailyMenus().size());
            }

            return request;
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'interpréter la réponse IA : " + e.getMessage(), e);
        }
    }

    private DishRequest generateAlternativeDishOnly(DishRequest dish) {
        try {
            String prompt = """
            Réponds uniquement avec un JSON valide.

            Génère un plat alternatif pour enfants allergiques.

            Plat original :
            {
              "mealType": "%s",
              "name": "%s",
              "description": "%s",
              "allergens": "%s"
            }

            Règles :
            - Même mealType que le plat original.
            - Ne contient aucun des allergènes du plat original.
            - Ressemble au plat original en apparence, texture et valeur nutritionnelle.
            - Adapté à une garderie tunisienne.
            - Doux, simple, non épicé.
            - Ne mets jamais le mot "alternative" dans le nom.
            - Mets toujours "allergens": "".

            Format exact :
            {
              "mealType": "%s",
              "name": "string",
              "description": "string",
              "allergens": ""
            }
            """.formatted(
                    dish.getMealType().name(),
                    safeText(dish.getName()),
                    safeText(dish.getDescription()),
                    safeText(dish.getAllergens()),
                    dish.getMealType().name()
            );

            ResponseEntity<Map> response = callOllama(prompt, 450);

            if (response.getBody() == null || response.getBody().get("response") == null) {
                return null;
            }

            String raw = response.getBody().get("response").toString()
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            JsonNode node = objectMapper.readTree(raw);

            return DishRequest.builder()
                    .mealType(dish.getMealType())
                    .name(node.path("name").asText(""))
                    .description(node.path("description").asText(""))
                    .allergens("")
                    .allergenConflictFlags("")
                    .build();

        } catch (Exception e) {
            System.out.println("⚠️ Alternative IA non générée : " + e.getMessage());
            return null;
        }
    }

    private String safeText(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private MealType safeMealType(String value) {
        if (value == null || value.isBlank()) {
            return MealType.ENTREE;
        }

        String normalized = value.trim().toUpperCase()
                .replace("PLAT PRINCIPAL", "PLAT_PRINCIPAL")
                .replace("PLAT_PRINCIPALE", "PLAT_PRINCIPAL")
                .replace("ENTRÉE", "ENTREE")
                .replace("GOÛTER", "GOUTER");

        try {
            return MealType.valueOf(normalized);
        } catch (Exception e) {
            return MealType.ENTREE;
        }
    }

    private static final java.util.Set<String> ALLOWED_ALLERGENS = java.util.Set.of(
            "gluten", "lait", "oeufs", "poisson", "arachides",
            "soja", "noix", "céleri", "moutarde", "sésame", "sulfites"
    );

    private String normalizeAllergens(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }

        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(ALLOWED_ALLERGENS::contains)
                .distinct()
                .collect(java.util.stream.Collectors.joining(", "));
    }
}