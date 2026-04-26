package com.tinyspring.garderie.repository.Events;

import com.tinyspring.garderie.entity.Events.MenuStatus;
import com.tinyspring.garderie.entity.Events.WeeklyMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

import java.util.List;

public interface WeeklyMenuRepository extends JpaRepository<WeeklyMenu, Long> {

    List<WeeklyMenu> findAllByOrderByWeekStartDateDesc();

    List<WeeklyMenu> findByStatusOrderByWeekStartDateDesc(MenuStatus status);
    Optional<WeeklyMenu> findByWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualAndStatus(
            LocalDate date1,
            LocalDate date2,
            MenuStatus status
    );
}