package com.tinyspring.garderie.service.Events;

import com.tinyspring.garderie.dto.Events.NotificationResponse;
import com.tinyspring.garderie.entity.Children.Child;
import com.tinyspring.garderie.entity.Events.*;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.repository.Children.ChildRepository;
import com.tinyspring.garderie.repository.Events.EventRepository;
import com.tinyspring.garderie.repository.Events.NotificationRepository;
import com.tinyspring.garderie.repository.Events.WeeklyMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParentNotificationService {

    private final NotificationRepository notificationRepository;
    private final WeeklyMenuRepository weeklyMenuRepository;
    private final ChildRepository childRepository;
    private final EventRepository eventRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long parentId) {
        return notificationRepository.findByParentOrdered(parentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countUnread(Long parentId) {
        return notificationRepository.countUnreadByParentId(parentId);
    }

    @Transactional
    public NotificationResponse markAsSeen(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification introuvable"));

        notification.setSeen(true);

        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllRead(Long parentId) {
        notificationRepository.markAllReadByParentId(parentId);
    }

    @Transactional
    public void notifyEventPublished(Event event) {
        if (event == null || event.getId() == null) {
            return;
        }

        if (event.getStatus() != EventStatus.PUBLISHED) {
            return;
        }

        Set<Long> classroomIds = resolveEventClassroomIds(event);

        if (classroomIds.isEmpty()) {
            System.out.println("EVENT NOTIFICATION SKIPPED: aucune classe cible pour event " + event.getId());
            return;
        }

        List<Child> children = childRepository.findByClassroomIdIn(classroomIds);

        if (children.isEmpty()) {
            System.out.println("EVENT NOTIFICATION SKIPPED: aucun enfant pour classes " + classroomIds);
            return;
        }

        Set<Long> handledParents = new HashSet<>();

        for (Child child : children) {
            if (child.getParent() == null) {
                continue;
            }

            User parent = child.getParent();
            Long parentId = parent.getId();

            if (!handledParents.add(parentId)) {
                continue;
            }

            String type = "EVENT_PUBLISHED_" + event.getId();

            if (notificationRepository.existsByParent_IdAndType(parentId, type)) {
                continue;
            }

            notificationRepository.save(
                    Notification.builder()
                            .parent(parent)
                            .title("Nouvel événement publié")
                            .message(buildEventPublishedMessage(event))
                            .type(type)
                            .priority("NORMAL")
                            .relatedEntityId(event.getId())
                            .relatedEntityType("EVENT")
                            .seen(false)
                            .createdAt(LocalDateTime.now())
                            .build()
            );

            System.out.println("EVENT NOTIFICATION CREATED: parent=" + parentId + ", event=" + event.getId());
        }
    }

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void sendEventPublishedNotificationsFallback() {
        System.out.println("SCHEDULE EVENT NOTIFICATIONS RUNNING");

        LocalDateTime since = LocalDateTime.now().minusHours(24);

        List<Event> events = eventRepository.findByStatusAndUpdatedAtAfter(
                EventStatus.PUBLISHED,
                since
        );

        for (Event event : events) {
            notifyEventPublished(event);
        }
    }

    @Scheduled(cron = "0 0 8 * * MON")
    @Transactional
    public void sendWeeklyAllergenSummary() {
        LocalDate today = LocalDate.now();

        weeklyMenuRepository
                .findByWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualAndStatus(
                        today,
                        today,
                        MenuStatus.PUBLISHED
                )
                .ifPresent(this::createWeeklyAllergenNotifications);
    }

    @Scheduled(cron = "0 16 16 * * *")
    @Transactional
    public void sendTomorrowAllergenAlerts() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        weeklyMenuRepository
                .findByWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualAndStatus(
                        tomorrow,
                        tomorrow,
                        MenuStatus.PUBLISHED
                )
                .ifPresent(menu -> createDailyAllergenNotifications(menu, tomorrow));
    }

    private void createWeeklyAllergenNotifications(WeeklyMenu weeklyMenu) {
        List<Child> children = childRepository.findByAllergiesIsNotNull();

        for (DailyMenu dailyMenu : weeklyMenu.getDailyMenus()) {
            if (!dailyMenu.isVisibleToParents()) {
                continue;
            }

            for (Dish dish : dailyMenu.getDishes()) {
                createAllergenNotificationsForDish(
                        weeklyMenu,
                        dailyMenu,
                        dish,
                        children,
                        "ALLERGEN_WEEKLY_" + dailyMenu.getId() + "_" + dish.getId()
                );
            }
        }
    }

    private void createDailyAllergenNotifications(WeeklyMenu weeklyMenu, LocalDate targetDate) {
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

        for (Dish dish : dailyMenu.getDishes()) {
            createAllergenNotificationsForDish(
                    weeklyMenu,
                    dailyMenu,
                    dish,
                    children,
                    "ALLERGEN_DAILY_" + dailyMenu.getId() + "_" + dish.getId()
            );
        }
    }

    private void createAllergenNotificationsForDish(
            WeeklyMenu weeklyMenu,
            DailyMenu dailyMenu,
            Dish dish,
            List<Child> children,
            String typePrefix
    ) {
        Set<String> dishAllergens = normalizeToSet(dish.getAllergens());

        if (dishAllergens.isEmpty()) {
            return;
        }

        for (Child child : children) {
            if (child.getParent() == null) {
                continue;
            }

            Set<String> childAllergies = normalizeToSet(child.getAllergies());

            for (String allergen : dishAllergens) {
                if (!childAllergies.contains(allergen)) {
                    continue;
                }

                Long parentId = child.getParent().getId();
                String type = typePrefix + "_" + child.getId() + "_" + allergen;

                if (notificationRepository.existsByParent_IdAndType(parentId, type)) {
                    continue;
                }

                notificationRepository.save(
                        Notification.builder()
                                .parent(child.getParent())
                                .title("Alerte allergène — " + formatDayLabel(dailyMenu.getMenuDate()))
                                .message(
                                        "Le plat " + dish.getName()
                                                + " contient " + capitalize(allergen)
                                                + " — " + child.getFirstName()
                                                + " " + child.getLastName()
                                                + " est allergique."
                                )
                                .type(type)
                                .priority("URGENT")
                                .relatedEntityId(weeklyMenu.getId())
                                .relatedEntityType("MENU")
                                .seen(false)
                                .createdAt(LocalDateTime.now())
                                .build()
                );
            }
        }
    }

    private String buildEventPublishedMessage(Event event) {
        StringBuilder message = new StringBuilder();

        message.append("L'événement ")
                .append(event.getTitle())
                .append(" est disponible");

        if (event.getStartDatetime() != null) {
            message.append(" le ")
                    .append(event.getStartDatetime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }

        if (StringUtils.hasText(event.getLocation())) {
            message.append(" à ").append(event.getLocation());
        }

        message.append(".");

        return message.toString();
    }

    private Set<Long> resolveEventClassroomIds(Event event) {
        Set<Long> ids = new LinkedHashSet<>();

        if (event.getClassroomId() != null) {
            ids.add(event.getClassroomId());
        }

        if (StringUtils.hasText(event.getTargetClassroomIds())) {
            Arrays.stream(event.getTargetClassroomIds().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(Long::valueOf)
                    .forEach(ids::add);
        }

        return ids;
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .seen(notification.isSeen())
                .createdAt(notification.getCreatedAt())
                .priority(notification.getPriority())
                .relatedEntityId(notification.getRelatedEntityId())
                .relatedEntityType(notification.getRelatedEntityType())
                .build();
    }

    private String formatDayLabel(LocalDate date) {
        if (date == null) {
            return "";
        }

        return date.format(DateTimeFormatter.ofPattern("EEEE dd MMMM", Locale.FRENCH));
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

    private String capitalize(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }

        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}