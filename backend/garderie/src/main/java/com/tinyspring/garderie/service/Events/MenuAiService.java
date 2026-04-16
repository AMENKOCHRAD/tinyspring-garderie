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
        Tu es un nutritionniste spécialisé en menus de garderie pour enfants de 2 à 11 ans en Tunisie.

        Ta mission est de générer un menu hebdomadaire réaliste, équilibré, simple et adapté à une garderie.
        Le menu doit convenir à de jeunes enfants : plats doux, faciles à manger, non épicés, nutritionnellement adaptés.

        Semaine de début : %s

        Réponds uniquement avec un JSON valide.
        Ne mets aucun texte avant ou après le JSON.

        Format exact attendu :
        {
          "title": "Menu semaine du 2026-04-27",
          "dailyMenus": [
            {
              "menuDate": "2026-04-27",
              "dayOfWeek": "MONDAY",
              "isVisibleToParents": true,
              "dishes": [
                {
                  "mealType": "ENTREE",
                  "name": "Soupe de légumes",
                  "description": "Soupe légère de légumes mixés adaptée aux enfants",
                  "allergens": "céleri"
                },
                {
                  "mealType": "PLAT_PRINCIPAL",
                  "name": "Escalope de poulet avec riz",
                  "description": "Poulet tendre accompagné de riz cuit simplement",
                  "allergens": ""
                },
                {
                  "mealType": "DESSERT",
                  "name": "Compote de pomme",
                  "description": "Compote douce sans morceaux",
                  "allergens": ""
                },
                {
                  "mealType": "GOUTER",
                  "name": "Pain au lait",
                  "description": "Petit pain moelleux adapté au goûter",
                  "allergens": "gluten, lait"
                }
              ]
            }
          ]
        }

        Contraintes obligatoires :
        - Générer  5 jours : MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY
        - Utiliser  4 plats par jour :
          1 ENTREE
          1 PLAT_PRINCIPAL
          1 DESSERT
          1 GOUTER
        - Les dates doivent correspondre exactement à la semaine demandée :
          jour 1 = %s
          jour 2 = %s
          jour 3 = %s
          jour 4 = %s
          jour 5 = %s
        - Les plats doivent être réalistes pour une garderie tunisienne
        - Les plats doivent être doux, simples, équilibrés, digestes, adaptés à des enfants de 2 à 6 ans
        - Utiliser des ingrédients courants et des préparations simples
        - Éviter les plats trop gras, trop épicés, trop salés ou difficiles à mâcher
        - Éviter les plats répétitifs sur la semaine

        Interdictions strictes :
        - Pas de harissa
        - Pas de merguez
        - Pas de plats très épicés
        - Pas de fritures lourdes
        - Pas de fruits secs entiers ou aliments dangereux pour de jeunes enfants
        - Pas de plats sophistiqués ou gastronomiques
        - Pas d'ingrédients incohérents comme allergènes

        Règles sur les allergènes :
        - Le champ "allergens" doit contenir uniquement une liste parmi :
          gluten, lait, oeufs, poisson, arachides, soja, noix, céleri, moutarde, sésame, sulfites
        - Si aucun allergène majeur n'est probable, mettre une chaîne vide ""
        - Ne jamais mettre dans "allergens" des ingrédients ordinaires comme :
          sucre, poivre, raisins, carottes, pommes de terre, oignons, semoule, fruits secs, farine
        - "allergens" doit être une chaîne de caractères séparée par des virgules

        Exemples de plats acceptables :
        - soupe de légumes
        - purée de pommes de terre
        - riz au poulet
        - pâtes sauce tomate douce
        - poisson au four
        - tajine doux aux légumes
        - compote
        - yaourt
        - fruit coupé
        - cake maison simple
        - tartine au fromage

        Vérifie avant de répondre :
        - JSON valide
        - 5 jours 
        - 4 plats par jour 
        - dates correctes
        - mealType uniquement parmi ENTREE, PLAT_PRINCIPAL, DESSERT, GOUTER
        - dayOfWeek uniquement parmi MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY
        """.formatted(
                weekStart,
                weekStart,
                weekStart.plusDays(1),
                weekStart.plusDays(2),
                weekStart.plusDays(3),
                weekStart.plusDays(4)
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