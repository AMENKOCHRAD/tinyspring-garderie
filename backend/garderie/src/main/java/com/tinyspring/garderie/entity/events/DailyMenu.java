    package com.tinyspring.garderie.entity.events;

    import jakarta.persistence.*;
    import lombok.*;

    import java.time.DayOfWeek;
    import java.time.LocalDate;
    import java.time.LocalDateTime;
    import java.util.ArrayList;
    import java.util.List;

    @Entity
    @Table(name = "daily_menu")
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public class DailyMenu {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "weekly_menu_id", nullable = false)
        private WeeklyMenu weeklyMenu;

        private LocalDate menuDate;

        @Enumerated(EnumType.STRING)
        private DayOfWeek dayOfWeek;

        private boolean isVisibleToParents;

        private LocalDateTime publishedAt;

        @OneToMany(mappedBy = "dailyMenu", cascade = CascadeType.ALL, orphanRemoval = true)
        @Builder.Default
        private List<Dish> dishes = new ArrayList<>();

        public void addDish(Dish dish) {
            dishes.add(dish);
            dish.setDailyMenu(this);
        }

        public void removeDish(Dish dish) {
            dishes.remove(dish);
            dish.setDailyMenu(null);
        }
    }