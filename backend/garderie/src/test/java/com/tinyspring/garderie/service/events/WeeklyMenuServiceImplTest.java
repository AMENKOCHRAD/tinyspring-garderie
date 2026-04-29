package com.tinyspring.garderie.service.events;

import com.tinyspring.garderie.dto.Events.DailyMenuRequest;
import com.tinyspring.garderie.dto.Events.DishRequest;
import com.tinyspring.garderie.dto.Events.WeeklyMenuRequest;
import com.tinyspring.garderie.dto.Events.WeeklyMenuResponse;
import com.tinyspring.garderie.entity.events.DailyMenu;
import com.tinyspring.garderie.entity.events.Dish;
import com.tinyspring.garderie.entity.events.MenuStatus;
import com.tinyspring.garderie.entity.events.WeeklyMenu;
import com.tinyspring.garderie.exception.Events.InvalidStatusTransitionException;
import com.tinyspring.garderie.exception.Events.ResourceNotFoundException;
import com.tinyspring.garderie.mapper.DailyMenuMapper;
import com.tinyspring.garderie.mapper.DishMapper;
import com.tinyspring.garderie.mapper.WeeklyMenuMapper;
import com.tinyspring.garderie.repository.events.WeeklyMenuRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeeklyMenuServiceImplTest {

    @Mock
    private WeeklyMenuRepository weeklyMenuRepository;

    @Mock
    private WeeklyMenuMapper weeklyMenuMapper;

    @Mock
    private DailyMenuMapper dailyMenuMapper;

    @Mock
    private DishMapper dishMapper;

    @InjectMocks
    private WeeklyMenuServiceImpl service;

    @Test
    void create_shouldCreateWeeklyMenuWithDefaultDraftStatusAndComputedEndDate() {
        WeeklyMenuRequest request = new WeeklyMenuRequest();
        request.setWeekStartDate(LocalDate.of(2026, 4, 27));

        WeeklyMenu weeklyMenu = new WeeklyMenu();
        weeklyMenu.setWeekStartDate(LocalDate.of(2026, 4, 27));

        WeeklyMenuResponse response = new WeeklyMenuResponse();

        when(weeklyMenuMapper.toEntity(request)).thenReturn(weeklyMenu);
        when(weeklyMenuRepository.save(weeklyMenu)).thenReturn(weeklyMenu);
        when(weeklyMenuMapper.toResponse(weeklyMenu)).thenReturn(response);

        WeeklyMenuResponse result = service.create(request);

        assertNotNull(result);
        assertEquals(MenuStatus.DRAFT, weeklyMenu.getStatus());
        assertEquals(LocalDate.of(2026, 5, 3), weeklyMenu.getWeekEndDate());

        verify(weeklyMenuRepository).save(weeklyMenu);
    }

    @Test
    void create_shouldConvertTemplateStatusToDraftAndKeepTemplateTrue() {
        WeeklyMenuRequest request = new WeeklyMenuRequest();

        WeeklyMenu weeklyMenu = new WeeklyMenu();
        weeklyMenu.setStatus(MenuStatus.TEMPLATE);

        WeeklyMenuResponse response = new WeeklyMenuResponse();

        when(weeklyMenuMapper.toEntity(request)).thenReturn(weeklyMenu);
        when(weeklyMenuRepository.save(weeklyMenu)).thenReturn(weeklyMenu);
        when(weeklyMenuMapper.toResponse(weeklyMenu)).thenReturn(response);

        service.create(request);

        assertTrue(weeklyMenu.isTemplate());
        assertEquals(MenuStatus.DRAFT, weeklyMenu.getStatus());
    }

    @Test
    void create_shouldBuildDailyMenusAndDishesGraph() {
        WeeklyMenuRequest request = new WeeklyMenuRequest();
        request.setWeekStartDate(LocalDate.of(2026, 4, 27));

        DailyMenuRequest dailyRequest = new DailyMenuRequest();
        dailyRequest.setIsVisibleToParents(true);

        DishRequest dishRequest = new DishRequest();
        dailyRequest.setDishes(List.of(dishRequest));

        request.setDailyMenus(List.of(dailyRequest));

        WeeklyMenu weeklyMenu = new WeeklyMenu();
        weeklyMenu.setWeekStartDate(LocalDate.of(2026, 4, 27));
        weeklyMenu.setDailyMenus(new ArrayList<>());

        DailyMenu dailyMenu = new DailyMenu();
        dailyMenu.setDishes(new ArrayList<>());

        Dish dish = new Dish();

        WeeklyMenuResponse response = new WeeklyMenuResponse();

        when(weeklyMenuMapper.toEntity(request)).thenReturn(weeklyMenu);
        when(dailyMenuMapper.toEntity(dailyRequest)).thenReturn(dailyMenu);
        when(dishMapper.toEntity(dishRequest)).thenReturn(dish);
        when(weeklyMenuRepository.save(weeklyMenu)).thenReturn(weeklyMenu);
        when(weeklyMenuMapper.toResponse(weeklyMenu)).thenReturn(response);

        service.create(request);

        assertEquals(1, weeklyMenu.getDailyMenus().size());
        assertEquals(LocalDate.of(2026, 4, 27), weeklyMenu.getDailyMenus().get(0).getMenuDate());
        assertEquals(DayOfWeek.MONDAY, weeklyMenu.getDailyMenus().get(0).getDayOfWeek());
        assertTrue(weeklyMenu.getDailyMenus().get(0).isVisibleToParents());
        assertEquals(1, weeklyMenu.getDailyMenus().get(0).getDishes().size());
    }

    @Test
    void getById_shouldReturnResponse_whenMenuExists() {
        WeeklyMenu menu = new WeeklyMenu();
        menu.setId(1L);

        WeeklyMenuResponse response = new WeeklyMenuResponse();

        when(weeklyMenuRepository.findById(1L)).thenReturn(Optional.of(menu));
        when(weeklyMenuMapper.toResponse(menu)).thenReturn(response);

        WeeklyMenuResponse result = service.getById(1L);

        assertSame(response, result);
    }

    @Test
    void getById_shouldThrow_whenMenuDoesNotExist() {
        when(weeklyMenuRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getById(1L));
    }

    @Test
    void update_shouldClearAndRebuildDailyMenus_whenDailyMenusProvided() {
        WeeklyMenuRequest request = new WeeklyMenuRequest();
        request.setWeekStartDate(LocalDate.of(2026, 5, 4));

        DailyMenuRequest dailyRequest = new DailyMenuRequest();
        dailyRequest.setMenuDate(LocalDate.of(2026, 5, 5));
        request.setDailyMenus(List.of(dailyRequest));

        WeeklyMenu existing = new WeeklyMenu();
        existing.setId(1L);
        existing.setWeekStartDate(LocalDate.of(2026, 4, 27));
        existing.setDailyMenus(new ArrayList<>());
        existing.getDailyMenus().add(new DailyMenu());

        DailyMenu newDailyMenu = new DailyMenu();
        newDailyMenu.setDishes(new ArrayList<>());

        WeeklyMenuResponse response = new WeeklyMenuResponse();

        when(weeklyMenuRepository.findById(1L)).thenReturn(Optional.of(existing));
        doAnswer(invocation -> {
            existing.setWeekStartDate(request.getWeekStartDate());
            return null;
        }).when(weeklyMenuMapper).updateEntityFromRequest(request, existing);

        when(dailyMenuMapper.toEntity(dailyRequest)).thenReturn(newDailyMenu);
        when(weeklyMenuRepository.save(existing)).thenReturn(existing);
        when(weeklyMenuMapper.toResponse(existing)).thenReturn(response);

        service.update(1L, request);

        assertEquals(1, existing.getDailyMenus().size());
        assertEquals(LocalDate.of(2026, 5, 5), existing.getDailyMenus().get(0).getMenuDate());
        assertEquals(LocalDate.of(2026, 5, 10), existing.getWeekEndDate());
    }

    @Test
    void delete_shouldDeleteMenu_whenExists() {
        WeeklyMenu menu = new WeeklyMenu();
        menu.setId(1L);

        when(weeklyMenuRepository.findById(1L)).thenReturn(Optional.of(menu));

        service.delete(1L);

        verify(weeklyMenuRepository).delete(menu);
    }

    @Test
    void duplicate_shouldDuplicateTemplateMenu() {
        WeeklyMenu source = new WeeklyMenu();
        source.setId(1L);
        source.setTitle("Menu semaine 1");
        source.setTemplate(true);
        source.setStatus(MenuStatus.DRAFT);
        source.setDailyMenus(new ArrayList<>());

        DailyMenu sourceDailyMenu = new DailyMenu();
        sourceDailyMenu.setDayOfWeek(DayOfWeek.MONDAY);
        sourceDailyMenu.setVisibleToParents(true);
        sourceDailyMenu.setDishes(new ArrayList<>());

        Dish sourceDish = new Dish();
        sourceDish.setName("Pâtes");
        sourceDish.setDescription("Pâtes tomate");
        sourceDish.setAllergens("gluten");

        sourceDailyMenu.getDishes().add(sourceDish);
        source.getDailyMenus().add(sourceDailyMenu);

        WeeklyMenuResponse response = new WeeklyMenuResponse();

        when(weeklyMenuRepository.findById(1L)).thenReturn(Optional.of(source));
        when(weeklyMenuRepository.save(any(WeeklyMenu.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(weeklyMenuMapper.toResponse(any(WeeklyMenu.class))).thenReturn(response);

        WeeklyMenuResponse result = service.duplicate(1L);

        assertNotNull(result);

        verify(weeklyMenuRepository).save(argThat(duplicate ->
                duplicate.getTitle().equals("Menu semaine 1 (copie)")
                        && duplicate.getStatus() == MenuStatus.DRAFT
                        && !duplicate.isTemplate()
                        && duplicate.getDailyMenus().size() == 1
                        && duplicate.getDailyMenus().get(0).getDishes().size() == 1
        ));
    }

    @Test
    void duplicate_shouldThrow_whenMenuIsNotTemplateAndNotPublished() {
        WeeklyMenu source = new WeeklyMenu();
        source.setId(1L);
        source.setTemplate(false);
        source.setStatus(MenuStatus.DRAFT);

        when(weeklyMenuRepository.findById(1L)).thenReturn(Optional.of(source));

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.duplicate(1L));

        verify(weeklyMenuRepository, never()).save(any());
    }

    @Test
    void getAll_shouldReturnMappedResponses() {
        WeeklyMenu menu = new WeeklyMenu();
        WeeklyMenuResponse response = new WeeklyMenuResponse();

        when(weeklyMenuRepository.findAllByOrderByWeekStartDateDesc())
                .thenReturn(List.of(menu));
        when(weeklyMenuMapper.toResponse(menu)).thenReturn(response);

        List<WeeklyMenuResponse> result = service.getAll();

        assertEquals(1, result.size());
        assertSame(response, result.get(0));
    }
}