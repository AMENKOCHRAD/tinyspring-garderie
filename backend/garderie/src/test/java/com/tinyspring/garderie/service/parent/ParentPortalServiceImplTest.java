package com.tinyspring.garderie.service.parent;

import com.tinyspring.garderie.dto.Events.*;
import com.tinyspring.garderie.dto.Parent.ParentChildResponse;
import com.tinyspring.garderie.entity.Children.Child;
import com.tinyspring.garderie.entity.Classes.Classe;
import com.tinyspring.garderie.entity.Role;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.events.*;
import com.tinyspring.garderie.exception.Events.InvalidStatusTransitionException;
import com.tinyspring.garderie.exception.Events.ResourceNotFoundException;
import com.tinyspring.garderie.mapper.EventMapper;
import com.tinyspring.garderie.mapper.EventRegistrationMapper;
import com.tinyspring.garderie.mapper.WeeklyMenuMapper;
import com.tinyspring.garderie.repository.Children.ChildRepository;
import com.tinyspring.garderie.repository.Classes.ClasseRepository;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.repository.events.EventRatingRepository;
import com.tinyspring.garderie.repository.events.EventRegistrationRepository;
import com.tinyspring.garderie.repository.events.EventRepository;
import com.tinyspring.garderie.repository.events.WeeklyMenuRepository;
import com.tinyspring.garderie.service.events.EventRegistrationService;
import com.tinyspring.garderie.service.events.EventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParentPortalServiceImplTest {

    @Mock private EventRatingRepository eventRatingRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChildRepository childRepository;
    @Mock private ClasseRepository classeRepository;
    @Mock private EventRepository eventRepository;
    @Mock private EventRegistrationRepository eventRegistrationRepository;
    @Mock private EventService eventService;
    @Mock private EventRegistrationService eventRegistrationService;
    @Mock private EventMapper eventMapper;
    @Mock private EventRegistrationMapper eventRegistrationMapper;
    @Mock private WeeklyMenuRepository weeklyMenuRepository;
    @Mock private WeeklyMenuMapper weeklyMenuMapper;

    @InjectMocks
    private ParentPortalServiceImpl service;

    @Test
    void getChildren_shouldReturnChildren_whenParentExists() {
        User parent = buildParent(1L);
        Classe classe = new Classe();
        classe.setId(2L);
        classe.setNom("Classe A");

        Child child = new Child();
        child.setId(10L);
        child.setFirstName("Ali");
        child.setLastName("Ben");
        child.setClassroom(classe);

        when(userRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(childRepository.findByParentIdOrderByFirstNameAscLastNameAsc(1L))
                .thenReturn(List.of(child));

        List<ParentChildResponse> result = service.getChildren(1L);

        assertEquals(1, result.size());
        assertEquals("Ali", result.get(0).getFirstName());
        assertEquals("Ben", result.get(0).getLastName());
        assertEquals(2L, result.get(0).getClassroomId());
        assertEquals("Classe A", result.get(0).getClassroomName());
    }

    @Test
    void getChildren_shouldThrow_whenParentNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getChildren(1L));
    }

    @Test
    void getChildren_shouldThrow_whenUserIsNotParent() {
        Role role = new Role();
        role.setName(RoleName.ADMIN);

        User user = new User();
        user.setId(1L);
        user.setRole(role);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.getChildren(1L));
    }

    @Test
    void getParticipations_shouldReturnMappedParticipations() {
        User parent = buildParent(1L);

        EventRegistration registration = new EventRegistration();
        registration.setId(100L);
        registration.setParentId(1L);
        registration.setChildId(10L);
        registration.setEventId(20L);
        registration.setStatus(RegistrationStatus.CONFIRMED);

        Event event = new Event();
        event.setId(20L);
        event.setTitle("Sortie");
        event.setStartDatetime(LocalDateTime.now().plusDays(3));

        EventRegistrationResponse response = new EventRegistrationResponse();

        when(userRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(eventRegistrationRepository.findByParentIdOrderByRegisteredAtDesc(1L))
                .thenReturn(List.of(registration));
        when(eventRegistrationMapper.toResponse(registration)).thenReturn(response);
        when(eventRepository.findById(20L)).thenReturn(Optional.of(event));
        when(childRepository.findById(10L)).thenReturn(Optional.empty());

        List<EventRegistrationResponse> result = service.getParticipations(1L);

        assertEquals(1, result.size());
        assertEquals("Sortie", result.get(0).getEventTitle());
        assertTrue(result.get(0).isCancellableByParent());
    }

    @Test
    void participate_shouldThrow_whenChildDoesNotBelongToParent() {
        User parent = buildParent(1L);

        User otherParent = new User();
        otherParent.setId(2L);

        Child child = new Child();
        child.setId(10L);
        child.setParent(otherParent);

        when(userRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(childRepository.findById(10L)).thenReturn(Optional.of(child));

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.participate(1L, 20L, 10L, null, null));
    }

    @Test
    void participate_shouldRegisterChild_whenEligibleAndNoExistingRegistration() {
        User parent = buildParent(1L);

        Classe classe = new Classe();
        classe.setId(2L);

        Child child = new Child();
        child.setId(10L);
        child.setParent(parent);
        child.setClassroom(classe);

        Event event = new Event();
        event.setId(20L);
        event.setClassroomId(2L);
        event.setStatus(EventStatus.PUBLISHED);

        EventRegistration saved = new EventRegistration();
        saved.setId(100L);
        saved.setEventId(20L);
        saved.setChildId(10L);
        saved.setParentId(1L);
        saved.setStatus(RegistrationStatus.CONFIRMED);

        EventRegistrationResponse response = new EventRegistrationResponse();

        when(userRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(childRepository.findById(10L)).thenReturn(Optional.of(child));
        when(eventRepository.findById(20L)).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.findByEventIdAndChildId(20L, 10L))
                .thenReturn(Optional.empty());
        when(eventRegistrationService.register(eq(20L), any(EventRegistrationRequest.class)))
                .thenReturn(saved);
        when(eventRegistrationMapper.toResponse(saved)).thenReturn(response);

        EventRegistrationResponse result =
                service.participate(1L, 20L, 10L, " note parent ", null);

        assertNotNull(result);
        verify(eventRegistrationService).register(eq(20L), argThat(request ->
                request.getChildId().equals(10L)
                        && request.getParentId().equals(1L)
                        && "note parent".equals(request.getNotes())
        ));
    }

    @Test
    void participate_shouldThrow_whenActiveRegistrationAlreadyExists() {
        User parent = buildParent(1L);

        Classe classe = new Classe();
        classe.setId(2L);

        Child child = new Child();
        child.setId(10L);
        child.setParent(parent);
        child.setClassroom(classe);

        Event event = new Event();
        event.setId(20L);
        event.setClassroomId(2L);

        EventRegistration existing = new EventRegistration();
        existing.setStatus(RegistrationStatus.CONFIRMED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(childRepository.findById(10L)).thenReturn(Optional.of(child));
        when(eventRepository.findById(20L)).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.findByEventIdAndChildId(20L, 10L))
                .thenReturn(Optional.of(existing));

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.participate(1L, 20L, 10L, null, null));
    }

    @Test
    void cancelParticipation_shouldCancel_whenValidAndMoreThan24HoursBeforeEvent() {
        User parent = buildParent(1L);

        EventRegistration registration = new EventRegistration();
        registration.setId(100L);
        registration.setParentId(1L);
        registration.setEventId(20L);
        registration.setChildId(10L);
        registration.setStatus(RegistrationStatus.CONFIRMED);

        Event event = new Event();
        event.setId(20L);
        event.setStartDatetime(LocalDateTime.now().plusDays(3));

        EventRegistration cancelled = new EventRegistration();
        cancelled.setId(100L);
        cancelled.setParentId(1L);
        cancelled.setEventId(20L);
        cancelled.setStatus(RegistrationStatus.CANCELLED);

        EventRegistrationResponse response = new EventRegistrationResponse();

        when(userRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(eventRegistrationRepository.findById(100L)).thenReturn(Optional.of(registration));
        when(eventRepository.findById(20L)).thenReturn(Optional.of(event));
        when(eventRegistrationService.cancel(100L)).thenReturn(cancelled);
        when(eventRegistrationMapper.toResponse(cancelled)).thenReturn(response);
        when(eventRepository.findById(cancelled.getEventId())).thenReturn(Optional.of(event));
        when(childRepository.findById(cancelled.getChildId())).thenReturn(Optional.empty());

        EventRegistrationResponse result = service.cancelParticipation(1L, 100L);

        assertNotNull(result);
        verify(eventRegistrationService).cancel(100L);
    }

    @Test
    void cancelParticipation_shouldThrow_whenRegistrationNotConfirmed() {
        User parent = buildParent(1L);

        EventRegistration registration = new EventRegistration();
        registration.setId(100L);
        registration.setParentId(1L);
        registration.setEventId(20L);
        registration.setStatus(RegistrationStatus.WAITLISTED);

        Event event = new Event();
        event.setId(20L);
        event.setStartDatetime(LocalDateTime.now().plusDays(3));

        when(userRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(eventRegistrationRepository.findById(100L)).thenReturn(Optional.of(registration));
        when(eventRepository.findById(20L)).thenReturn(Optional.of(event));

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.cancelParticipation(1L, 100L));

        verify(eventRegistrationService, never()).cancel(anyLong());
    }

    @Test
    void cancelParticipation_shouldThrow_whenLessThan24HoursBeforeEvent() {
        User parent = buildParent(1L);

        EventRegistration registration = new EventRegistration();
        registration.setId(100L);
        registration.setParentId(1L);
        registration.setEventId(20L);
        registration.setStatus(RegistrationStatus.CONFIRMED);

        Event event = new Event();
        event.setId(20L);
        event.setStartDatetime(LocalDateTime.now().plusHours(5));

        when(userRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(eventRegistrationRepository.findById(100L)).thenReturn(Optional.of(registration));
        when(eventRepository.findById(20L)).thenReturn(Optional.of(event));

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.cancelParticipation(1L, 100L));
    }

    @Test
    void rateEvent_shouldCreateRating_whenChildAttendedCompletedEvent() {
        User parent = buildParent(1L);

        Child child = new Child();
        child.setId(10L);
        child.setParent(parent);

        Event event = new Event();
        event.setId(20L);
        event.setStatus(EventStatus.COMPLETED);

        EventRegistration attended = new EventRegistration();
        attended.setEventId(20L);
        attended.setChildId(10L);
        attended.setStatus(RegistrationStatus.ATTENDED);

        EventRatingRequest request = new EventRatingRequest();
        request.setChildId(10L);
        request.setStars(5);
        request.setComment(" Très bien ");

        EventRating saved = EventRating.builder()
                .id(200L)
                .eventId(20L)
                .childId(10L)
                .parentId(1L)
                .stars(5)
                .comment("Très bien")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(childRepository.findById(10L)).thenReturn(Optional.of(child));
        when(eventRepository.findById(20L)).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.findByChildIdInAndStatus(Set.of(10L), RegistrationStatus.ATTENDED))
                .thenReturn(List.of(attended));
        when(eventRatingRepository.findByEventIdAndChildId(20L, 10L))
                .thenReturn(Optional.empty());
        when(eventRatingRepository.save(any(EventRating.class))).thenReturn(saved);

        EventRatingResponse result = service.rateEvent(1L, 20L, request);

        assertEquals(5, result.getStars());
        assertEquals("Très bien", result.getComment());
        verify(eventRatingRepository).save(any(EventRating.class));
    }

    @Test
    void rateEvent_shouldThrow_whenEventNotCompleted() {
        User parent = buildParent(1L);

        Child child = new Child();
        child.setId(10L);
        child.setParent(parent);

        Event event = new Event();
        event.setId(20L);
        event.setStatus(EventStatus.PUBLISHED);

        EventRatingRequest request = new EventRatingRequest();
        request.setChildId(10L);
        request.setStars(4);

        when(userRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(childRepository.findById(10L)).thenReturn(Optional.of(child));
        when(eventRepository.findById(20L)).thenReturn(Optional.of(event));

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.rateEvent(1L, 20L, request));
    }

    @Test
    void rateEvent_shouldThrow_whenChildDidNotAttend() {
        User parent = buildParent(1L);

        Child child = new Child();
        child.setId(10L);
        child.setParent(parent);

        Event event = new Event();
        event.setId(20L);
        event.setStatus(EventStatus.COMPLETED);

        EventRatingRequest request = new EventRatingRequest();
        request.setChildId(10L);
        request.setStars(4);

        when(userRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(childRepository.findById(10L)).thenReturn(Optional.of(child));
        when(eventRepository.findById(20L)).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.findByChildIdInAndStatus(Set.of(10L), RegistrationStatus.ATTENDED))
                .thenReturn(List.of());

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.rateEvent(1L, 20L, request));
    }

    @Test
    void getMenus_shouldReturnPublishedMenusWithAllergenConflicts() {
        User parent = buildParent(1L);

        Child child = new Child();
        child.setId(10L);
        child.setFirstName("Ali");
        child.setLastName("Ben");
        child.setAllergies("gluten");
        child.setParent(parent);

        Dish dish = new Dish();
        dish.setId(30L);
        dish.setName("Pâtes");
        dish.setAllergens("gluten");

        DailyMenu dailyMenu = new DailyMenu();
        dailyMenu.setId(40L);
        dailyMenu.setDishes(List.of(dish));

        WeeklyMenu menu = new WeeklyMenu();
        menu.setId(50L);
        menu.setStatus(MenuStatus.PUBLISHED);
        menu.setDailyMenus(List.of(dailyMenu));

        DailyMenuResponse dailyResponse = new DailyMenuResponse();
        dailyResponse.setId(40L);

        WeeklyMenuResponse menuResponse = new WeeklyMenuResponse();
        menuResponse.setId(50L);
        menuResponse.setDailyMenus(List.of(dailyResponse));

        when(userRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(childRepository.findByParentIdOrderByFirstNameAscLastNameAsc(1L))
                .thenReturn(List.of(child));
        when(weeklyMenuRepository.findByStatusOrderByWeekStartDateDesc(MenuStatus.PUBLISHED))
                .thenReturn(List.of(menu));
        when(weeklyMenuMapper.toResponse(menu)).thenReturn(menuResponse);

        List<WeeklyMenuResponse> result = service.getMenus(1L);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getDailyMenus().get(0).getAllergenConflictFlags().size());
        assertEquals(1, result.get(0).getDailyMenus().get(0).getAllergenConflictMessages().size());
        assertTrue(result.get(0).getDailyMenus().get(0).getAllergenConflictMessages().get(0).contains("gluten"));
    }
    @Test
    void getEvents_shouldReturnEvents() {

        Role role = new Role();
        role.setName(RoleName.PARENT);

        User parent = new User();
        parent.setId(1L);
        parent.setRole(role);

        when(userRepository.findById(1L)).thenReturn(Optional.of(parent));

        List<?> result = service.getEvents(1L);

        assertNotNull(result);
    }

    @Test
    void getEvents_shouldThrow_whenUserIsNotParent() {
        Role role = new Role();
        role.setName(RoleName.ADMIN);

        User user = new User();
        user.setId(1L);
        user.setRole(role);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.getEvents(1L));

        verify(childRepository, never()).findByParentIdOrderByFirstNameAscLastNameAsc(anyLong());
    }

    @Test
    void getMenus_shouldReturnEmpty_whenNoMenus() {

        Role role = new Role();
        role.setName(RoleName.PARENT);

        User parent = new User();
        parent.setId(1L);
        parent.setRole(role);

        when(userRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(childRepository.findByParentIdOrderByFirstNameAscLastNameAsc(1L))
                .thenReturn(List.of());
        when(weeklyMenuRepository.findByStatusOrderByWeekStartDateDesc(any()))
                .thenReturn(List.of());

        List<?> result = service.getMenus(1L);

        assertNotNull(result);
    }

    private User buildParent(Long id) {
        Role role = new Role();
        role.setName(RoleName.PARENT);

        User parent = new User();
        parent.setId(id);
        parent.setRole(role);

        return parent;
    }
}