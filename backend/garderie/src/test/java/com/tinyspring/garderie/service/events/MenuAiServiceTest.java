package com.tinyspring.garderie.service.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyspring.garderie.dto.Events.DishRequest;
import com.tinyspring.garderie.dto.Events.WeeklyMenuAiGenerateRequest;
import com.tinyspring.garderie.dto.Events.WeeklyMenuRequest;
import com.tinyspring.garderie.entity.events.MealType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuAiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AlternativeDishService alternativeDishService;

    private MenuAiService service;

    @BeforeEach
    void setUp() {
        service = new MenuAiService(
                new ObjectMapper(),
                restTemplate,
                alternativeDishService
        );
    }

    @Test
    void shouldGenerateWeeklyMenu_whenAiResponseIsValid() {
        WeeklyMenuAiGenerateRequest request = new WeeklyMenuAiGenerateRequest();
        request.setWeekStartDate("2026-04-27");

        String fakeJson = """
        {
          "title": "Menu test",
          "dailyMenus": [
            {
              "isVisibleToParents": true,
              "dishes": [
                {"mealType":"ENTREE","name":"Soupe","description":"soupe","allergens":""},
                {"mealType":"PLAT_PRINCIPAL","name":"Pâtes","description":"pâtes","allergens":"gluten"},
                {"mealType":"DESSERT","name":"Yaourt","description":"yaourt","allergens":"lait"},
                {"mealType":"GOUTER","name":"Fruit","description":"fruit","allergens":""}
              ]
            },
            {
              "isVisibleToParents": true,
              "dishes": [
                {"mealType":"ENTREE","name":"Soupe","description":"soupe","allergens":""},
                {"mealType":"PLAT_PRINCIPAL","name":"Riz","description":"riz","allergens":""},
                {"mealType":"DESSERT","name":"Compote","description":"compote","allergens":""},
                {"mealType":"GOUTER","name":"Fruit","description":"fruit","allergens":""}
              ]
            },
            {
              "isVisibleToParents": true,
              "dishes": [
                {"mealType":"ENTREE","name":"Soupe","description":"soupe","allergens":""},
                {"mealType":"PLAT_PRINCIPAL","name":"Riz","description":"riz","allergens":""},
                {"mealType":"DESSERT","name":"Compote","description":"compote","allergens":""},
                {"mealType":"GOUTER","name":"Fruit","description":"fruit","allergens":""}
              ]
            },
            {
              "isVisibleToParents": true,
              "dishes": [
                {"mealType":"ENTREE","name":"Soupe","description":"soupe","allergens":""},
                {"mealType":"PLAT_PRINCIPAL","name":"Riz","description":"riz","allergens":""},
                {"mealType":"DESSERT","name":"Compote","description":"compote","allergens":""},
                {"mealType":"GOUTER","name":"Fruit","description":"fruit","allergens":""}
              ]
            },
            {
              "isVisibleToParents": true,
              "dishes": [
                {"mealType":"ENTREE","name":"Soupe","description":"soupe","allergens":""},
                {"mealType":"PLAT_PRINCIPAL","name":"Riz","description":"riz","allergens":""},
                {"mealType":"DESSERT","name":"Compote","description":"compote","allergens":""},
                {"mealType":"GOUTER","name":"Fruit","description":"fruit","allergens":""}
              ]
            }
          ]
        }
        """;

        when(restTemplate.exchange(
                anyString(),
                any(),
                any(),
                any(Class.class)
        )).thenReturn(ResponseEntity.ok(Map.of("response", fakeJson)));

        when(alternativeDishService.generateAlternativeFor(any(DishRequest.class)))
                .thenAnswer(invocation -> {
                    DishRequest dish = invocation.getArgument(0);
                    return DishRequest.builder()
                            .mealType(dish.getMealType())
                            .name("Alternative " + dish.getName())
                            .description("Alternative adaptée")
                            .allergens("")
                            .allergenConflictFlags("PLAT_ADAPTE")
                            .build();
                });

        WeeklyMenuRequest result = service.generateWeeklyMenuDraft(request);

        assertNotNull(result);
        assertEquals("Menu test", result.getTitle());
        assertEquals(5, result.getDailyMenus().size());

        assertEquals(6, result.getDailyMenus().get(0).getDishes().size());
        assertEquals(MealType.PLAT_PRINCIPAL, result.getDailyMenus().get(0).getDishes().get(1).getMealType());

        verify(restTemplate).exchange(anyString(), any(), any(), any(Class.class));
        verify(alternativeDishService, times(2)).generateAlternativeFor(any(DishRequest.class));
    }

    @Test
    void shouldThrow_whenAiResponseBodyIsNull() {
        WeeklyMenuAiGenerateRequest request = new WeeklyMenuAiGenerateRequest();
        request.setWeekStartDate("2026-04-27");

        when(restTemplate.exchange(
                anyString(),
                any(),
                any(),
                any(Class.class)
        )).thenReturn(ResponseEntity.ok(null));

        assertThrows(IllegalStateException.class,
                () -> service.generateWeeklyMenuDraft(request));
    }

    @Test
    void shouldThrow_whenAiResponseFieldIsMissing() {
        WeeklyMenuAiGenerateRequest request = new WeeklyMenuAiGenerateRequest();
        request.setWeekStartDate("2026-04-27");

        when(restTemplate.exchange(
                anyString(),
                any(),
                any(),
                any(Class.class)
        )).thenReturn(ResponseEntity.ok(Map.of("wrong", "value")));

        assertThrows(IllegalStateException.class,
                () -> service.generateWeeklyMenuDraft(request));
    }

    @Test
    void shouldThrow_whenJsonIsInvalid() {
        WeeklyMenuAiGenerateRequest request = new WeeklyMenuAiGenerateRequest();
        request.setWeekStartDate("2026-04-27");

        when(restTemplate.exchange(
                anyString(),
                any(),
                any(),
                any(Class.class)
        )).thenReturn(ResponseEntity.ok(Map.of("response", "INVALID_JSON")));

        assertThrows(IllegalStateException.class,
                () -> service.generateWeeklyMenuDraft(request));
    }
}