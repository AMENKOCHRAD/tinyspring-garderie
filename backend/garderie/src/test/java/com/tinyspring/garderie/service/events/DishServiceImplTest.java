package com.tinyspring.garderie.service.events;

import com.tinyspring.garderie.dto.Events.DishRequest;
import com.tinyspring.garderie.dto.Events.DishResponse;
import com.tinyspring.garderie.entity.events.DailyMenu;
import com.tinyspring.garderie.entity.events.Dish;
import com.tinyspring.garderie.exception.Events.ResourceNotFoundException;
import com.tinyspring.garderie.mapper.DishMapper;
import com.tinyspring.garderie.repository.events.DailyMenuRepository;
import com.tinyspring.garderie.repository.events.DishRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DishServiceImplTest {

    @Mock
    private DishRepository dishRepository;

    @Mock
    private DailyMenuRepository dailyMenuRepository;

    @Mock
    private DishMapper dishMapper;

    @InjectMocks
    private DishServiceImpl dishService;

    @Test
    void create_shouldCreateDish_whenDailyMenuExists() {
        DishRequest request = new DishRequest();
        request.setDailyMenuId(1L);

        DailyMenu dailyMenu = new DailyMenu();
        dailyMenu.setId(1L);

        Dish dish = new Dish();
        Dish savedDish = new Dish();
        DishResponse response = new DishResponse();

        when(dailyMenuRepository.findById(1L)).thenReturn(Optional.of(dailyMenu));
        when(dishMapper.toEntity(request)).thenReturn(dish);
        when(dishRepository.save(dish)).thenReturn(savedDish);
        when(dishMapper.toResponse(savedDish)).thenReturn(response);

        DishResponse result = dishService.create(request);

        assertSame(response, result);
        assertSame(dailyMenu, dish.getDailyMenu());

        verify(dishRepository).save(dish);
    }

    @Test
    void create_shouldThrowIllegalArgumentException_whenDailyMenuIdIsNull() {
        DishRequest request = new DishRequest();

        assertThrows(IllegalArgumentException.class,
                () -> dishService.create(request));

        verify(dishRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowResourceNotFoundException_whenDailyMenuDoesNotExist() {
        DishRequest request = new DishRequest();
        request.setDailyMenuId(99L);

        when(dailyMenuRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> dishService.create(request));

        verify(dishRepository, never()).save(any());
    }

    @Test
    void update_shouldUpdateDishWithoutChangingDailyMenu_whenDailyMenuIdIsNull() {
        DishRequest request = new DishRequest();

        Dish dish = new Dish();
        dish.setId(10L);

        Dish savedDish = new Dish();
        DishResponse response = new DishResponse();

        when(dishRepository.findById(10L)).thenReturn(Optional.of(dish));
        when(dishRepository.save(dish)).thenReturn(savedDish);
        when(dishMapper.toResponse(savedDish)).thenReturn(response);

        DishResponse result = dishService.update(10L, request);

        assertSame(response, result);

        verify(dishMapper).updateEntityFromRequest(request, dish);
        verify(dailyMenuRepository, never()).findById(anyLong());
        verify(dishRepository).save(dish);
    }

    @Test
    void update_shouldUpdateDishAndChangeDailyMenu_whenDailyMenuIdProvided() {
        DishRequest request = new DishRequest();
        request.setDailyMenuId(2L);

        Dish dish = new Dish();
        dish.setId(10L);

        DailyMenu newDailyMenu = new DailyMenu();
        newDailyMenu.setId(2L);

        Dish savedDish = new Dish();
        DishResponse response = new DishResponse();

        when(dishRepository.findById(10L)).thenReturn(Optional.of(dish));
        when(dailyMenuRepository.findById(2L)).thenReturn(Optional.of(newDailyMenu));
        when(dishRepository.save(dish)).thenReturn(savedDish);
        when(dishMapper.toResponse(savedDish)).thenReturn(response);

        DishResponse result = dishService.update(10L, request);

        assertSame(response, result);
        assertSame(newDailyMenu, dish.getDailyMenu());

        verify(dishMapper).updateEntityFromRequest(request, dish);
        verify(dishRepository).save(dish);
    }

    @Test
    void update_shouldThrowResourceNotFoundException_whenDishDoesNotExist() {
        DishRequest request = new DishRequest();

        when(dishRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> dishService.update(10L, request));

        verify(dishRepository, never()).save(any());
    }

    @Test
    void delete_shouldDeleteDish_whenDishExists() {
        Dish dish = new Dish();
        dish.setId(10L);

        when(dishRepository.findById(10L)).thenReturn(Optional.of(dish));

        dishService.delete(10L);

        verify(dishRepository).delete(dish);
    }

    @Test
    void delete_shouldThrowResourceNotFoundException_whenDishDoesNotExist() {
        when(dishRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> dishService.delete(10L));

        verify(dishRepository, never()).delete(any());
    }

    @Test
    void getByDailyMenuId_shouldReturnMappedDishes() {
        Dish dish1 = new Dish();
        Dish dish2 = new Dish();

        DishResponse response1 = new DishResponse();
        DishResponse response2 = new DishResponse();

        when(dishRepository.findByDailyMenu_IdOrderByMealTypeAscNameAsc(1L))
                .thenReturn(List.of(dish1, dish2));
        when(dishMapper.toResponse(dish1)).thenReturn(response1);
        when(dishMapper.toResponse(dish2)).thenReturn(response2);

        List<DishResponse> result = dishService.getByDailyMenuId(1L);

        assertEquals(2, result.size());
        assertSame(response1, result.get(0));
        assertSame(response2, result.get(1));
    }
}