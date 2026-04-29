package com.tinyspring.garderie.service.events;

import com.tinyspring.garderie.dto.Events.NotificationResponse;
import com.tinyspring.garderie.entity.Children.Child;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.events.*;
import com.tinyspring.garderie.repository.Children.ChildRepository;
import com.tinyspring.garderie.repository.events.EventRepository;
import com.tinyspring.garderie.repository.events.NotificationRepository;
import com.tinyspring.garderie.repository.events.WeeklyMenuRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParentNotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private WeeklyMenuRepository weeklyMenuRepository;

    @Mock
    private ChildRepository childRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private ParentNotificationService service;

    @Test
    void getNotifications_shouldReturnMappedResponses() {
        Notification notification = Notification.builder()
                .id(1L)
                .title("Titre")
                .message("Message")
                .type("TYPE")
                .priority("NORMAL")
                .seen(false)
                .createdAt(LocalDateTime.now())
                .relatedEntityId(10L)
                .relatedEntityType("EVENT")
                .build();

        when(notificationRepository.findByParentOrdered(5L))
                .thenReturn(List.of(notification));

        List<NotificationResponse> result = service.getNotifications(5L);

        assertEquals(1, result.size());
        assertEquals("Titre", result.get(0).getTitle());
        assertEquals("Message", result.get(0).getMessage());
        assertEquals("TYPE", result.get(0).getType());
        assertFalse(result.get(0).isSeen());
    }

    @Test
    void countUnread_shouldReturnRepositoryCount() {
        when(notificationRepository.countUnreadByParentId(5L)).thenReturn(3L);

        long result = service.countUnread(5L);

        assertEquals(3L, result);
    }

    @Test
    void markAsSeen_shouldMarkNotificationAsSeen() {
        Notification notification = Notification.builder()
                .id(1L)
                .title("Notif")
                .seen(false)
                .build();

        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationResponse result = service.markAsSeen(1L);

        assertTrue(notification.isSeen());
        assertTrue(result.isSeen());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsSeen_shouldThrow_whenNotificationNotFound() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.markAsSeen(1L));
    }

    @Test
    void markAllRead_shouldCallRepository() {
        service.markAllRead(5L);

        verify(notificationRepository).markAllReadByParentId(5L);
    }

    @Test
    void notifyEventPublished_shouldDoNothing_whenEventIsNull() {
        service.notifyEventPublished(null);

        verifyNoInteractions(childRepository);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void notifyEventPublished_shouldDoNothing_whenEventIsNotPublished() {
        Event event = new Event();
        event.setId(1L);
        event.setStatus(EventStatus.DRAFT);

        service.notifyEventPublished(event);

        verifyNoInteractions(childRepository);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void notifyEventPublished_shouldCreateNotificationForEachParentOnlyOnce() {
        Event event = new Event();
        event.setId(10L);
        event.setTitle("Sortie");
        event.setStatus(EventStatus.PUBLISHED);
        event.setClassroomId(2L);
        event.setStartDatetime(LocalDateTime.of(2026, 4, 29, 10, 0));
        event.setLocation("Tunis");

        User parent = new User();
        parent.setId(100L);

        Child child1 = new Child();
        child1.setId(1L);
        child1.setParent(parent);

        Child child2 = new Child();
        child2.setId(2L);
        child2.setParent(parent);

        when(childRepository.findByClassroomIdIn(anySet()))
                .thenReturn(List.of(child1, child2));

        when(notificationRepository.existsByParent_IdAndType(100L, "EVENT_PUBLISHED_10"))
                .thenReturn(false);

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.notifyEventPublished(event);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void notifyEventPublished_shouldNotCreateDuplicateNotification() {
        Event event = new Event();
        event.setId(10L);
        event.setStatus(EventStatus.PUBLISHED);
        event.setClassroomId(2L);

        User parent = new User();
        parent.setId(100L);

        Child child = new Child();
        child.setParent(parent);

        when(childRepository.findByClassroomIdIn(anySet()))
                .thenReturn(List.of(child));

        when(notificationRepository.existsByParent_IdAndType(100L, "EVENT_PUBLISHED_10"))
                .thenReturn(true);

        service.notifyEventPublished(event);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void sendEventPublishedNotificationsFallback_shouldNotifyRecentPublishedEvents() {
        Event event = new Event();
        event.setId(10L);
        event.setStatus(EventStatus.PUBLISHED);
        event.setClassroomId(2L);

        when(eventRepository.findByStatusAndUpdatedAtAfter(eq(EventStatus.PUBLISHED), any(LocalDateTime.class)))
                .thenReturn(List.of(event));

        when(childRepository.findByClassroomIdIn(anySet()))
                .thenReturn(List.of());

        service.sendEventPublishedNotificationsFallback();

        verify(eventRepository).findByStatusAndUpdatedAtAfter(eq(EventStatus.PUBLISHED), any(LocalDateTime.class));
    }

    @Test
    void sendWeeklyAllergenSummary_shouldCreateAllergenNotification() {
        WeeklyMenu weeklyMenu = buildWeeklyMenuWithAllergenDish(LocalDate.now());

        User parent = new User();
        parent.setId(50L);

        Child child = new Child();
        child.setId(7L);
        child.setFirstName("Ali");
        child.setLastName("Ben");
        child.setAllergies("gluten");
        child.setParent(parent);

        when(weeklyMenuRepository.findByWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualAndStatus(
                any(LocalDate.class), any(LocalDate.class), eq(MenuStatus.PUBLISHED)))
                .thenReturn(Optional.of(weeklyMenu));

        when(childRepository.findByAllergiesIsNotNull())
                .thenReturn(List.of(child));

        when(notificationRepository.existsByParent_IdAndType(eq(50L), anyString()))
                .thenReturn(false);

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.sendWeeklyAllergenSummary();

        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
    }

    @Test
    void sendTomorrowAllergenAlerts_shouldCreateNotificationForTomorrowMenu() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        WeeklyMenu weeklyMenu = buildWeeklyMenuWithAllergenDish(tomorrow);

        User parent = new User();
        parent.setId(50L);

        Child child = new Child();
        child.setId(7L);
        child.setFirstName("Sara");
        child.setLastName("Ben");
        child.setAllergies("gluten");
        child.setParent(parent);

        when(weeklyMenuRepository.findByWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualAndStatus(
                any(LocalDate.class), any(LocalDate.class), eq(MenuStatus.PUBLISHED)))
                .thenReturn(Optional.of(weeklyMenu));

        when(childRepository.findByAllergiesIsNotNull())
                .thenReturn(List.of(child));

        when(notificationRepository.existsByParent_IdAndType(eq(50L), anyString()))
                .thenReturn(false);

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.sendTomorrowAllergenAlerts();

        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
    }

    @Test
    void sendTomorrowAllergenAlerts_shouldNotCreateNotification_whenNoMenuExists() {
        when(weeklyMenuRepository.findByWeekStartDateLessThanEqualAndWeekEndDateGreaterThanEqualAndStatus(
                any(LocalDate.class), any(LocalDate.class), eq(MenuStatus.PUBLISHED)))
                .thenReturn(Optional.empty());

        service.sendTomorrowAllergenAlerts();

        verify(notificationRepository, never()).save(any());
    }

    private WeeklyMenu buildWeeklyMenuWithAllergenDish(LocalDate menuDate) {
        WeeklyMenu weeklyMenu = new WeeklyMenu();
        weeklyMenu.setId(1L);
        weeklyMenu.setStatus(MenuStatus.PUBLISHED);

        DailyMenu dailyMenu = new DailyMenu();
        dailyMenu.setId(2L);
        dailyMenu.setMenuDate(menuDate);
        dailyMenu.setVisibleToParents(true);

        Dish dish = new Dish();
        dish.setId(3L);
        dish.setName("Pâtes");
        dish.setAllergens("gluten");

        dailyMenu.setDishes(List.of(dish));
        weeklyMenu.setDailyMenus(List.of(dailyMenu));

        return weeklyMenu;
    }
}