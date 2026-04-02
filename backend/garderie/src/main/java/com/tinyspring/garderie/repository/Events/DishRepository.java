package com.tinyspring.garderie.repository.Events;

import com.tinyspring.garderie.entity.Events.Dish;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DishRepository extends JpaRepository<Dish, Long> {
    List<Dish> findByDailyMenuId(Long dailyMenuId);
}
