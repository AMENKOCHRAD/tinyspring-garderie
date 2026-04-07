package com.tinyspring.garderie.repository.Events;

import com.tinyspring.garderie.entity.Events.DailyMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DailyMenuRepository extends JpaRepository<DailyMenu, Long> {
    List<DailyMenu> findByWeeklyMenu_IdOrderByMenuDateAsc(Long weeklyMenuId);
}