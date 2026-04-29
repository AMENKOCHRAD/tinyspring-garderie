package com.tinyspring.garderie.repository.events;

import com.tinyspring.garderie.entity.events.Dish;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DishRepository extends JpaRepository<Dish, Long> {
    List<Dish> findByDailyMenu_Id(Long dailyMenuId);
    List<Dish> findByDailyMenu_IdOrderByMealTypeAscNameAsc(Long dailyMenuId);
}