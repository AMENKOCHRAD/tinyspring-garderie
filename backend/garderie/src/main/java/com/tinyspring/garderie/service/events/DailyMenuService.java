package com.tinyspring.garderie.service.events;

import com.tinyspring.garderie.dto.Events.DailyMenuRequest;
import com.tinyspring.garderie.dto.Events.DailyMenuResponse;

import java.util.List;

public interface DailyMenuService {
    DailyMenuResponse create(DailyMenuRequest request);
    DailyMenuResponse update(Long id, DailyMenuRequest request);
    void delete(Long id);
    List<DailyMenuResponse> getByWeeklyMenuId(Long weeklyMenuId);
}
