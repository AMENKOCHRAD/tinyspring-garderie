package com.tinyspring.garderie.mapper;

import com.tinyspring.garderie.dto.Events.DailyMenuRequest;
import com.tinyspring.garderie.dto.Events.DailyMenuResponse;
import com.tinyspring.garderie.entity.events.DailyMenu;
import com.tinyspring.garderie.entity.events.WeeklyMenu;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DailyMenuMapperTest {

    private final DailyMenuMapper mapper = Mappers.getMapper(DailyMenuMapper.class);

    @Test
    void toEntity_shouldMapDailyMenuRequestToEntity() {
        DailyMenuRequest request = new DailyMenuRequest();
        request.setMenuDate(LocalDate.of(2026, 4, 29));
        request.setDayOfWeek(DayOfWeek.WEDNESDAY);
        request.setIsVisibleToParents(true);

        DailyMenu result = mapper.toEntity(request);

        assertNotNull(result);
        assertNull(result.getId());
        assertEquals(LocalDate.of(2026, 4, 29), result.getMenuDate());
        assertEquals(DayOfWeek.WEDNESDAY, result.getDayOfWeek());
        assertTrue(result.isVisibleToParents());
        assertNull(result.getWeeklyMenu());
    }

    @Test
    void toResponse_shouldMapDailyMenuToResponse() {
        WeeklyMenu weeklyMenu = new WeeklyMenu();
        weeklyMenu.setId(5L);

        DailyMenu dailyMenu = new DailyMenu();
        dailyMenu.setId(1L);
        dailyMenu.setMenuDate(LocalDate.of(2026, 4, 29));
        dailyMenu.setDayOfWeek(DayOfWeek.WEDNESDAY);
        dailyMenu.setVisibleToParents(true);
        dailyMenu.setWeeklyMenu(weeklyMenu);

        DailyMenuResponse result = mapper.toResponse(dailyMenu);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(5L, result.getWeeklyMenuId());
        assertEquals(LocalDate.of(2026, 4, 29), result.getMenuDate());
        assertEquals(DayOfWeek.WEDNESDAY, result.getDayOfWeek());
        assertTrue(result.getIsVisibleToParents());
    }

    @Test
    void updateEntityFromRequest_shouldUpdateDailyMenu() {
        DailyMenu dailyMenu = new DailyMenu();
        dailyMenu.setId(1L);
        dailyMenu.setVisibleToParents(true);

        DailyMenuRequest request = new DailyMenuRequest();
        request.setMenuDate(LocalDate.of(2026, 5, 1));
        request.setDayOfWeek(DayOfWeek.FRIDAY);
        request.setIsVisibleToParents(false);

        mapper.updateEntityFromRequest(request, dailyMenu);

        assertEquals(1L, dailyMenu.getId());
        assertEquals(LocalDate.of(2026, 5, 1), dailyMenu.getMenuDate());
        assertEquals(DayOfWeek.FRIDAY, dailyMenu.getDayOfWeek());
        assertFalse(dailyMenu.isVisibleToParents());
    }
}