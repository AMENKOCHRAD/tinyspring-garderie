package com.tinyspring.garderie.service.events;

import com.tinyspring.garderie.dto.Events.DishRequest;

public interface AlternativeDishService {
    DishRequest generateAlternativeFor(DishRequest originalDish);
}
