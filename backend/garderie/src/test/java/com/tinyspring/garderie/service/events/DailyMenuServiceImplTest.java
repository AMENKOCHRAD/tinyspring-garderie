package com.tinyspring.garderie.service.events;

import com.tinyspring.garderie.dto.Events.DailyMenuRequest;
import com.tinyspring.garderie.dto.Events.DailyMenuResponse;
import com.tinyspring.garderie.entity.events.DailyMenu;
import com.tinyspring.garderie.entity.events.WeeklyMenu;
import com.tinyspring.garderie.exception.Events.ResourceNotFoundException;
import com.tinyspring.garderie.mappeer.DailyMenuMapper;
import com.tinyspring.garderie.repository.events.DailyMenuRepository;
import com.tinyspring.garderie.repository.events.WeeklyMenuRepository;
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
class DailyMenuServiceImplTest {

    @Mock
    private DailyMenuRepository dailyMenuRepository;

    @Mock
    private WeeklyMenuRepository weeklyMenuRepository;

    @Mock
    private DailyMenuMapper dailyMenuMapper;

    @InjectMocks
    private DailyMenuServiceImpl dailyMenuService;

    @Test
    void create_shouldCreateDailyMenu_whenWeeklyMenuExists() {
        DailyMenuRequest request = new DailyMenuRequest();
        request.setWeeklyMenuId(1L);
        request.setIsVisibleToParents(true);

        WeeklyMenu weeklyMenu = new WeeklyMenu();
        weeklyMenu.setId(1L);

        DailyMenu dailyMenu = new DailyMenu();
        DailyMenu savedDailyMenu = new DailyMenu();
        DailyMenuResponse response = new DailyMenuResponse();

        when(weeklyMenuRepository.findById(1L)).thenReturn(Optional.of(weeklyMenu));
        when(dailyMenuMapper.toEntity(request)).thenReturn(dailyMenu);
        when(dailyMenuRepository.save(dailyMenu)).thenReturn(savedDailyMenu);
        when(dailyMenuMapper.toResponse(savedDailyMenu)).thenReturn(response);

        DailyMenuResponse result = dailyMenuService.create(request);

        assertSame(response, result);
        assertSame(weeklyMenu, dailyMenu.getWeeklyMenu());
        assertTrue(dailyMenu.isVisibleToParents());

        verify(dailyMenuRepository).save(dailyMenu);
    }

    @Test
    void create_shouldSetVisibleToParentsFalse_whenRequestValueIsNull() {
        DailyMenuRequest request = new DailyMenuRequest();
        request.setWeeklyMenuId(1L);
        request.setIsVisibleToParents(null);

        WeeklyMenu weeklyMenu = new WeeklyMenu();
        DailyMenu dailyMenu = new DailyMenu();
        DailyMenuResponse response = new DailyMenuResponse();

        when(weeklyMenuRepository.findById(1L)).thenReturn(Optional.of(weeklyMenu));
        when(dailyMenuMapper.toEntity(request)).thenReturn(dailyMenu);
        when(dailyMenuRepository.save(dailyMenu)).thenReturn(dailyMenu);
        when(dailyMenuMapper.toResponse(dailyMenu)).thenReturn(response);

        dailyMenuService.create(request);

        assertFalse(dailyMenu.isVisibleToParents());
    }

    @Test
    void create_shouldThrowIllegalArgumentException_whenWeeklyMenuIdIsNull() {
        DailyMenuRequest request = new DailyMenuRequest();

        assertThrows(IllegalArgumentException.class,
                () -> dailyMenuService.create(request));

        verify(dailyMenuRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowResourceNotFoundException_whenWeeklyMenuDoesNotExist() {
        DailyMenuRequest request = new DailyMenuRequest();
        request.setWeeklyMenuId(99L);

        when(weeklyMenuRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> dailyMenuService.create(request));

        verify(dailyMenuRepository, never()).save(any());
    }

    @Test
    void update_shouldUpdateDailyMenuWithoutChangingWeeklyMenu_whenWeeklyMenuIdIsNull() {
        DailyMenuRequest request = new DailyMenuRequest();
        request.setIsVisibleToParents(true);

        DailyMenu dailyMenu = new DailyMenu();
        dailyMenu.setId(10L);

        DailyMenu savedDailyMenu = new DailyMenu();
        DailyMenuResponse response = new DailyMenuResponse();

        when(dailyMenuRepository.findById(10L)).thenReturn(Optional.of(dailyMenu));
        when(dailyMenuRepository.save(dailyMenu)).thenReturn(savedDailyMenu);
        when(dailyMenuMapper.toResponse(savedDailyMenu)).thenReturn(response);

        DailyMenuResponse result = dailyMenuService.update(10L, request);

        assertSame(response, result);
        assertTrue(dailyMenu.isVisibleToParents());

        verify(dailyMenuMapper).updateEntityFromRequest(request, dailyMenu);
        verify(weeklyMenuRepository, never()).findById(anyLong());
        verify(dailyMenuRepository).save(dailyMenu);
    }

    @Test
    void update_shouldUpdateDailyMenuAndChangeWeeklyMenu_whenWeeklyMenuIdProvided() {
        DailyMenuRequest request = new DailyMenuRequest();
        request.setWeeklyMenuId(2L);
        request.setIsVisibleToParents(false);

        DailyMenu dailyMenu = new DailyMenu();
        dailyMenu.setId(10L);
        dailyMenu.setVisibleToParents(true);

        WeeklyMenu newWeeklyMenu = new WeeklyMenu();
        newWeeklyMenu.setId(2L);

        DailyMenu savedDailyMenu = new DailyMenu();
        DailyMenuResponse response = new DailyMenuResponse();

        when(dailyMenuRepository.findById(10L)).thenReturn(Optional.of(dailyMenu));
        when(weeklyMenuRepository.findById(2L)).thenReturn(Optional.of(newWeeklyMenu));
        when(dailyMenuRepository.save(dailyMenu)).thenReturn(savedDailyMenu);
        when(dailyMenuMapper.toResponse(savedDailyMenu)).thenReturn(response);

        DailyMenuResponse result = dailyMenuService.update(10L, request);

        assertSame(response, result);
        assertSame(newWeeklyMenu, dailyMenu.getWeeklyMenu());
        assertFalse(dailyMenu.isVisibleToParents());

        verify(dailyMenuMapper).updateEntityFromRequest(request, dailyMenu);
        verify(dailyMenuRepository).save(dailyMenu);
    }

    @Test
    void update_shouldKeepVisibilityUnchanged_whenVisibilityIsNull() {
        DailyMenuRequest request = new DailyMenuRequest();
        request.setIsVisibleToParents(null);

        DailyMenu dailyMenu = new DailyMenu();
        dailyMenu.setId(10L);
        dailyMenu.setVisibleToParents(true);

        DailyMenuResponse response = new DailyMenuResponse();

        when(dailyMenuRepository.findById(10L)).thenReturn(Optional.of(dailyMenu));
        when(dailyMenuRepository.save(dailyMenu)).thenReturn(dailyMenu);
        when(dailyMenuMapper.toResponse(dailyMenu)).thenReturn(response);

        dailyMenuService.update(10L, request);

        assertTrue(dailyMenu.isVisibleToParents());
    }

    @Test
    void update_shouldThrowResourceNotFoundException_whenDailyMenuDoesNotExist() {
        DailyMenuRequest request = new DailyMenuRequest();

        when(dailyMenuRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> dailyMenuService.update(10L, request));

        verify(dailyMenuRepository, never()).save(any());
    }

    @Test
    void update_shouldThrowResourceNotFoundException_whenNewWeeklyMenuDoesNotExist() {
        DailyMenuRequest request = new DailyMenuRequest();
        request.setWeeklyMenuId(99L);

        DailyMenu dailyMenu = new DailyMenu();
        dailyMenu.setId(10L);

        when(dailyMenuRepository.findById(10L)).thenReturn(Optional.of(dailyMenu));
        when(weeklyMenuRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> dailyMenuService.update(10L, request));

        verify(dailyMenuRepository, never()).save(any());
    }

    @Test
    void delete_shouldDeleteDailyMenu_whenDailyMenuExists() {
        DailyMenu dailyMenu = new DailyMenu();
        dailyMenu.setId(10L);

        when(dailyMenuRepository.findById(10L)).thenReturn(Optional.of(dailyMenu));

        dailyMenuService.delete(10L);

        verify(dailyMenuRepository).delete(dailyMenu);
    }

    @Test
    void delete_shouldThrowResourceNotFoundException_whenDailyMenuDoesNotExist() {
        when(dailyMenuRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> dailyMenuService.delete(10L));

        verify(dailyMenuRepository, never()).delete(any());
    }

    @Test
    void getByWeeklyMenuId_shouldReturnMappedDailyMenus() {
        DailyMenu dailyMenu1 = new DailyMenu();
        DailyMenu dailyMenu2 = new DailyMenu();

        DailyMenuResponse response1 = new DailyMenuResponse();
        DailyMenuResponse response2 = new DailyMenuResponse();

        when(dailyMenuRepository.findByWeeklyMenu_IdOrderByMenuDateAsc(1L))
                .thenReturn(List.of(dailyMenu1, dailyMenu2));
        when(dailyMenuMapper.toResponse(dailyMenu1)).thenReturn(response1);
        when(dailyMenuMapper.toResponse(dailyMenu2)).thenReturn(response2);

        List<DailyMenuResponse> result = dailyMenuService.getByWeeklyMenuId(1L);

        assertEquals(2, result.size());
        assertSame(response1, result.get(0));
        assertSame(response2, result.get(1));
    }
}