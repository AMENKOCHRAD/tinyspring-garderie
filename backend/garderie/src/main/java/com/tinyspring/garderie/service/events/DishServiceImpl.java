package com.tinyspring.garderie.service.events;

import com.tinyspring.garderie.dto.Events.DishRequest;
import com.tinyspring.garderie.dto.Events.DishResponse;
import com.tinyspring.garderie.entity.events.DailyMenu;
import com.tinyspring.garderie.entity.events.Dish;
import com.tinyspring.garderie.exception.Events.ResourceNotFoundException;
import com.tinyspring.garderie.mapper.DishMapper;
import com.tinyspring.garderie.repository.events.DailyMenuRepository;
import com.tinyspring.garderie.repository.events.DishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DishServiceImpl implements DishService {

    private final DishRepository dishRepository;
    private final DailyMenuRepository dailyMenuRepository;
    private final DishMapper dishMapper;

    @Override
    public DishResponse create(DishRequest request) {
        if (request.getDailyMenuId() == null) {
            throw new IllegalArgumentException("dailyMenuId est obligatoire pour la creation d'un plat");
        }

        DailyMenu dailyMenu = getDailyMenuEntity(request.getDailyMenuId());
        Dish dish = dishMapper.toEntity(request);
        dish.setDailyMenu(dailyMenu);

        return dishMapper.toResponse(dishRepository.save(dish));
    }

    @Override
    public DishResponse update(Long id, DishRequest request) {
        Dish dish = getEntity(id);
        dishMapper.updateEntityFromRequest(request, dish);

        if (request.getDailyMenuId() != null) {
            DailyMenu dailyMenu = getDailyMenuEntity(request.getDailyMenuId());
            dish.setDailyMenu(dailyMenu);
        }

        return dishMapper.toResponse(dishRepository.save(dish));
    }

    @Override
    public void delete(Long id) {
        Dish dish = getEntity(id);
        dishRepository.delete(dish);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DishResponse> getByDailyMenuId(Long dailyMenuId) {
        return dishRepository.findByDailyMenu_IdOrderByMealTypeAscNameAsc(dailyMenuId)
                .stream()
                .map(dishMapper::toResponse)
                .toList();
    }

    private Dish getEntity(Long id) {
        return dishRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plat introuvable avec l'id : " + id));
    }

    private DailyMenu getDailyMenuEntity(Long dailyMenuId) {
        return dailyMenuRepository.findById(dailyMenuId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu journalier introuvable avec l'id : " + dailyMenuId));
    }
}
