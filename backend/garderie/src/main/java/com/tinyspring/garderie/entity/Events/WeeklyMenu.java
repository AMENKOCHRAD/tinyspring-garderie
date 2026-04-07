package com.tinyspring.garderie.entity.Events;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "weekly_menu")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private LocalDate weekStartDate;
    private LocalDate weekEndDate;

    @Enumerated(EnumType.STRING)
    private MenuStatus status;

    private boolean isTemplate;

    private String templateName;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "weeklyMenu", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DailyMenu> dailyMenus = new ArrayList<>();

    public void addDailyMenu(DailyMenu dailyMenu) {
        dailyMenus.add(dailyMenu);
        dailyMenu.setWeeklyMenu(this);
    }

    public void removeDailyMenu(DailyMenu dailyMenu) {
        dailyMenus.remove(dailyMenu);
        dailyMenu.setWeeklyMenu(null);
    }
}