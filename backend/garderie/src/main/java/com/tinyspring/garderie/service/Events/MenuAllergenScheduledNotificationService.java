package com.tinyspring.garderie.service.Events;

import com.tinyspring.garderie.entity.Children.Child;
import com.tinyspring.garderie.entity.Events.*;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.repository.Children.ChildRepository;
import com.tinyspring.garderie.repository.Events.NotificationRepository;
import com.tinyspring.garderie.repository.Events.WeeklyMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuAllergenScheduledNotificationService {
    private final WeeklyMenuRepository weeklyMenuRepository;
    private final ChildRepository childRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public void sendWeeklyAllergenSummary() {
        LocalDate today = LocalDate.now();

        weeklyMenuRepository
                .findByWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualAndStatus(
                        today,
                        today,
                        MenuStatus.PUBLISHED
                )
                .ifPresent(this::createWeeklyNotifications);
    }

    @Transactional
    public void sendTomorrowAllergenAlerts() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        weeklyMenuRepository
                .findByWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualAndStatus(
                        tomorrow,
                        tomorrow,
                        MenuStatus.PUBLISHED
                )
                .ifPresent(menu -> createDailyNotifications(menu, tomorrow));
    }

    private void createWeeklyNotifications(WeeklyMenu weeklyMenu) {
        Map<Long, ParentAlertBucket> alertsByParent = new LinkedHashMap<>();
        List<Child> children = childRepository.findByAllergiesIsNotNull();

        for (DailyMenu dailyMenu : weeklyMenu.getDailyMenus()) {
            if (!dailyMenu.isVisibleToParents()) {
                continue;
            }

            for (Dish dish : dailyMenu.getDishes()) {
                Set<String> dishAllergens = normalizeToSet(dish.getAllergens());

                for (Child child : children) {
                    Set<String> childAllergies = normalizeToSet(child.getAllergies());

                    for (String allergen : dishAllergens) {
                        if (childAllergies.contains(allergen)) {
                            notificationRepository.save(
                                    Notification.builder()
                                            .parent(child.getParent())
                                            .title("Alerte allergène")
                                            .message(
                                                    dish.getName()
                                                            + " contient "
                                                            + allergen
                                                            + " — "
                                                            + child.getFirstName()
                                                            + " est allergique."
                                            )
                                            .type("ALLERGEN")
                                            .seen(false)
                                            .createdAt(LocalDateTime.now())
                                            .build()
                            );
                        }
                    }
                }
            }
        }

        alertsByParent.forEach((parentId, bucket) -> {
            String type = "MENU_ALLERGEN_WEEKLY_" + weeklyMenu.getId();

            if (notificationRepository.existsByParentIdAndType(parentId, type)) {
                return;
            }

            Notification notification = Notification.builder()
                    .parent(bucket.parent)
                    .title("Alerte allergène - menu de la semaine")
                    .message("Résumé des allergènes détectés cette semaine :\n\n" + String.join("\n", bucket.messages))
                    .type(type)
                    .seen(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);
        });
    }

    private void createDailyNotifications(WeeklyMenu weeklyMenu, LocalDate targetDate) {
        Optional<DailyMenu> optionalDailyMenu = weeklyMenu.getDailyMenus()
                .stream()
                .filter(day -> targetDate.equals(day.getMenuDate()))
                .filter(DailyMenu::isVisibleToParents)
                .findFirst();

        if (optionalDailyMenu.isEmpty()) {
            return;
        }

        DailyMenu dailyMenu = optionalDailyMenu.get();
        List<Child> children = childRepository.findByAllergiesIsNotNull();
        Map<Long, ParentAlertBucket> alertsByParent = new LinkedHashMap<>();

        for (Dish dish : dailyMenu.getDishes()) {
            Set<String> dishAllergens = normalizeToSet(dish.getAllergens());

            for (Child child : children) {
                Set<String> childAllergies = normalizeToSet(child.getAllergies());

                for (String allergen : dishAllergens) {
                    if (childAllergies.contains(allergen)) {
                        Long parentId = child.getParent().getId();

                        alertsByParent
                                .computeIfAbsent(parentId, id -> new ParentAlertBucket(child.getParent()))
                                .messages
                                .add(
                                        dish.getName()
                                                + " contient du "
                                                + allergen
                                                + " — "
                                                + child.getFirstName()
                                                + " "
                                                + child.getLastName()
                                                + " est allergique."
                                );
                    }
                }
            }
        }

        alertsByParent.forEach((parentId, bucket) -> {
            String type = "MENU_ALLERGEN_DAILY_" + dailyMenu.getId();

            if (notificationRepository.existsByParentIdAndType(parentId, type)) {
                return;
            }

            Notification notification = Notification.builder()
                    .parent(bucket.parent)
                    .title("Alerte allergène - menu de demain")
                    .message("Attention, le menu de demain contient des allergènes :\n\n" + String.join("\n", bucket.messages))
                    .type(type)
                    .seen(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationRepository.save(notification);
        });
    }

    private Set<String> normalizeToSet(String value) {
        if (!StringUtils.hasText(value)) {
            return Set.of();
        }

        return Arrays.stream(value.split("[,;\\n]"))
                .map(this::normalizeText)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeText(String value) {
        return Normalizer.normalize(value.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    private static class ParentAlertBucket {
        private final User parent;
        private final Set<String> messages = new LinkedHashSet<>();

        private ParentAlertBucket(User parent) {
            this.parent = parent;
        }
    }

}
