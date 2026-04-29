package com.tinyspring.garderie.mapper;

import com.tinyspring.garderie.dto.Events.DishRequest;
import com.tinyspring.garderie.dto.Events.DishResponse;
import com.tinyspring.garderie.entity.events.DailyMenu;
import com.tinyspring.garderie.entity.events.Dish;
import com.tinyspring.garderie.entity.events.MealType;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

class DishMapperTest {

    private final DishMapper mapper = Mappers.getMapper(DishMapper.class);

    @Test
    void toEntity_shouldMapDishRequestToEntity() {
        DishRequest request = new DishRequest();
        request.setMealType(MealType.PLAT_PRINCIPAL);
        request.setName("Pâtes");
        request.setDescription("Pâtes tomate");
        request.setAllergens("gluten");

        Dish result = mapper.toEntity(request);

        assertNotNull(result);
        assertNull(result.getId());
        assertEquals(MealType.PLAT_PRINCIPAL, result.getMealType());
        assertEquals("Pâtes", result.getName());
        assertEquals("Pâtes tomate", result.getDescription());
        assertEquals("gluten", result.getAllergens());
        assertNull(result.getDailyMenu());
    }

    @Test
    void toResponse_shouldMapDishToResponse() {
        DailyMenu dailyMenu = new DailyMenu();
        dailyMenu.setId(7L);

        Dish dish = new Dish();
        dish.setId(1L);
        dish.setMealType(MealType.DESSERT);
        dish.setName("Yaourt");
        dish.setDescription("Yaourt nature");
        dish.setAllergens("lait");
        dish.setDailyMenu(dailyMenu);

        DishResponse result = mapper.toResponse(dish);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(7L, result.getDailyMenuId());
        assertEquals(MealType.DESSERT, result.getMealType());
        assertEquals("Yaourt", result.getName());
        assertEquals("Yaourt nature", result.getDescription());
        assertEquals("lait", result.getAllergens());
    }

    @Test
    void updateEntityFromRequest_shouldUpdateDish() {
        Dish dish = new Dish();
        dish.setId(1L);

        DishRequest request = new DishRequest();
        request.setMealType(MealType.GOUTER);
        request.setName("Fruit");
        request.setDescription("Pomme");
        request.setAllergens("");

        mapper.updateEntityFromRequest(request, dish);

        assertEquals(1L, dish.getId());
        assertEquals(MealType.GOUTER, dish.getMealType());
        assertEquals("Fruit", dish.getName());
        assertEquals("Pomme", dish.getDescription());
        assertEquals("", dish.getAllergens());
    }
}