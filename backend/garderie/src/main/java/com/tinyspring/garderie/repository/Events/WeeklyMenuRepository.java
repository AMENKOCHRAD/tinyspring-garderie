package com.tinyspring.garderie.repository.Events;

import com.tinyspring.garderie.entity.Events.WeeklyMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeeklyMenuRepository extends JpaRepository<WeeklyMenu, Long> {
    List<WeeklyMenu> findAllByOrderByWeekStartDateDesc();
}