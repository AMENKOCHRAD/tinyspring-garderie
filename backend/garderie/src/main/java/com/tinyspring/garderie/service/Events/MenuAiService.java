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
import java.util.List;
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

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "model", "mistral",
                    "prompt", prompt,
                    "stream", false,
                    "format", "json"
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "http://localhost:11434/api/generate",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

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

    private String buildPrompt(LocalDate weekStart) {
        return """
        Tu génères uniquement du JSON valide.

    Contexte :
    Tu es un nutritionniste spécialisé en menus de garderie en Tunisie pour enfants de 2 à 6 ans.

    Objectif :
    Générer un menu hebdomadaire simple, réaliste, doux, équilibré, non épicé.

    Semaine :
    - MONDAY = %s
    - TUESDAY = %s
    - WEDNESDAY = %s
    - THURSDAY = %s
    - FRIDAY = %s

    Règles obligatoires :
    - Répondre avec UN SEUL objet JSON
    - Aucun texte avant ou après
    - JSON strictement valide
    - 5 éléments dans dailyMenus
    - 1 jour par élément
    - 4 plats par jour exactement
    - mealType autorisé uniquement :
      ENTREE, PLAT_PRINCIPAL, DESSERT, GOUTER
    - dayOfWeek autorisé uniquement :
      MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY
    - allergens autorisés uniquement :
      gluten, lait, oeufs, poisson, arachides, soja, noix, céleri, moutarde, sésame, sulfites
    - Si aucun allergène : ""

    Interdictions :
    - pas de harissa
    - pas de merguez
    - pas de plats épicés
    - pas de fritures lourdes
    - pas de plats sophistiqués
    - pas d’allergènes inventés
    - pas d’ingrédients ordinaires dans allergens

    Structure JSON attendue :
    {
      "title": "Menu semaine du %s",
      "dailyMenus": [
        {
          "menuDate": "%s",
          "dayOfWeek": "MONDAY",
          "isVisibleToParents": true,
          "dishes": [
            {
              "mealType": "ENTREE",
              "name": "string",
              "description": "string",
              "allergens": ""
            },
            {
              "mealType": "PLAT_PRINCIPAL",
              "name": "string",
              "description": "string",
              "allergens": ""
            },
            {
              "mealType": "DESSERT",
              "name": "string",
              "description": "string",
              "allergens": ""
            },
            {
              "mealType": "GOUTER",
              "name": "string",
              "description": "string",
              "allergens": ""
            }
          ]
        }
      ]
    }

    Vérification interne avant réponse :
    - le JSON est valide
    - tous les crochets et accolades sont fermés
    - dailyMenus contient exactement 5 objets
    - chaque objet contient exactement 4 dishes
    - aucune valeur hors liste autorisée
    - si le JSON n’est pas valide, corrige-le avant d’envoyer

    Réponds maintenant avec le JSON uniquement.
    """.formatted(
                weekStart,
                weekStart.plusDays(1),
                weekStart.plusDays(2),
                weekStart.plusDays(3),
                weekStart.plusDays(4),
                weekStart,
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

                LocalDate computedDate = weekStart.plusDays(i);
                DayOfWeek computedDayOfWeek = orderedDays[i];

                DailyMenuRequest dailyMenu = DailyMenuRequest.builder()
                        .menuDate(computedDate)
                        .dayOfWeek(computedDayOfWeek)
                        .isVisibleToParents(dayNode.path("isVisibleToParents").asBoolean(true))
                        .dishes(new ArrayList<>())
                        .build();

                JsonNode dishesNode = dayNode.path("dishes");
                if (dishesNode.isArray()) {
                    for (JsonNode dishNode : dishesNode) {
                        DishRequest dish = DishRequest.builder()
                                .mealType(safeMealType(dishNode.path("mealType").asText()))
                                .name(dishNode.path("name").asText(""))
                                .description(dishNode.path("description").asText(""))
                                .allergens(normalizeAllergens(dishNode.path("allergens").asText("")))
                                .allergenConflictFlags("")
                                .build();

                        dailyMenu.getDishes().add(dish);
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