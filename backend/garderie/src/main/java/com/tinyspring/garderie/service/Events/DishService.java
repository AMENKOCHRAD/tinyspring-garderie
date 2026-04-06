package com.tinyspring.garderie.service.Events;

import com.tinyspring.garderie.dto.Events.DishRequest;
import com.tinyspring.garderie.dto.Events.DishResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DishService {
    DishResponse create(DishRequest request);
    DishResponse update(Long id, DishRequest request);
    void delete(Long id);
    DishResponse uploadPhoto(Long id, MultipartFile file);
    List<DishResponse> getByDailyMenuId(Long dailyMenuId);
}
