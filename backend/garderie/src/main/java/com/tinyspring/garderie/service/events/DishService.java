package com.tinyspring.garderie.service.events;

import com.tinyspring.garderie.dto.Events.DishRequest;
import com.tinyspring.garderie.dto.Events.DishResponse;

import java.util.List;

public interface DishService {
    DishResponse create(DishRequest request);
    DishResponse update(Long id, DishRequest request);
    void delete(Long id);
    List<DishResponse> getByDailyMenuId(Long dailyMenuId);
}
