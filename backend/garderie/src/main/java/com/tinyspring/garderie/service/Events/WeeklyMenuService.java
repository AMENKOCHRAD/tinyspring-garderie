package com.tinyspring.garderie.service.Events;

import com.tinyspring.garderie.dto.Events.WeeklyMenuRequest;
import com.tinyspring.garderie.dto.Events.WeeklyMenuResponse;

import java.util.List;

public interface WeeklyMenuService {
    List<WeeklyMenuResponse> getAll();
    WeeklyMenuResponse getById(Long id);
    WeeklyMenuResponse create(WeeklyMenuRequest request);
    WeeklyMenuResponse update(Long id, WeeklyMenuRequest request);
    void delete(Long id);
    WeeklyMenuResponse duplicate(Long id);
}
