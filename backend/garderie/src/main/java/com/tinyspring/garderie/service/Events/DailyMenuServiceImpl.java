package com.tinyspring.garderie.service.Events;

import com.tinyspring.garderie.dto.Events.DailyMenuRequest;
import com.tinyspring.garderie.dto.Events.DailyMenuResponse;
import com.tinyspring.garderie.dto.Events.DishResponse;
import com.tinyspring.garderie.entity.Events.DailyMenu;
import com.tinyspring.garderie.exception.Events.ResourceNotFoundException;
import com.tinyspring.garderie.mappeer.DailyMenuMapper;
import com.tinyspring.garderie.repository.Events.DailyMenuRepository;
import com.tinyspring.garderie.repository.Events.WeeklyMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyMenuServiceImpl implements DailyMenuService {
    private final DailyMenuRepository dailyMenuRepository;
    private final WeeklyMenuRepository weeklyMenuRepository;
    private final DishService dishService;
    private final DailyMenuMapper dailyMenuMapper;

    @Override
    public DailyMenuResponse create(DailyMenuRequest request) {
        ensureWeeklyMenuExists(request.getWeeklyMenuId());
        DailyMenu dailyMenu = dailyMenuMapper.toEntity(request);
        return toResponse(dailyMenuRepository.save(dailyMenu));
    }

    @Override
    public DailyMenuResponse update(Long id, DailyMenuRequest request) {
        ensureWeeklyMenuExists(request.getWeeklyMenuId());
        DailyMenu dailyMenu = getEntity(id);
        dailyMenuMapper.updateEntityFromRequest(request, dailyMenu);
        return toResponse(dailyMenuRepository.save(dailyMenu));
    }

    @Override
    public void delete(Long id) {
        DailyMenu dailyMenu = getEntity(id);
        dailyMenuRepository.delete(dailyMenu);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyMenuResponse> getByWeeklyMenuId(Long weeklyMenuId) {
        ensureWeeklyMenuExists(weeklyMenuId);
        return dailyMenuRepository.findByWeeklyMenuIdOrderByMenuDate(weeklyMenuId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private DailyMenu getEntity(Long id) {
        return dailyMenuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu journalier introuvable avec l'id : " + id));
    }

    private void ensureWeeklyMenuExists(Long weeklyMenuId) {
        weeklyMenuRepository.findById(weeklyMenuId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu hebdomadaire introuvable avec l'id : " + weeklyMenuId));
    }

    private DailyMenuResponse toResponse(DailyMenu dailyMenu) {
        DailyMenuResponse response = dailyMenuMapper.toResponse(dailyMenu);
        List<DishResponse> dishes = dishService.getByDailyMenuId(dailyMenu.getId());
        response.setDishes(dishes);
        return response;
    }
}
