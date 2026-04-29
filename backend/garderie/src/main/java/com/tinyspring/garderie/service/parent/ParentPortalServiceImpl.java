package com.tinyspring.garderie.service.parent;

import com.tinyspring.garderie.dto.Events.*;
import com.tinyspring.garderie.dto.Parent.ParentChildResponse;
import com.tinyspring.garderie.entity.Children.Child;
import com.tinyspring.garderie.entity.Classes.Classe;
import com.tinyspring.garderie.entity.events.*;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.exception.Events.InvalidStatusTransitionException;
import com.tinyspring.garderie.exception.Events.ResourceNotFoundException;
import com.tinyspring.garderie.mapper.EventMapper;
import com.tinyspring.garderie.mapper.EventRegistrationMapper;
import com.tinyspring.garderie.mapper.WeeklyMenuMapper;
import com.tinyspring.garderie.repository.Children.ChildRepository;
import com.tinyspring.garderie.repository.Classes.ClasseRepository;
import com.tinyspring.garderie.repository.events.EventRegistrationRepository;
import com.tinyspring.garderie.repository.events.EventRepository;
import com.tinyspring.garderie.repository.events.WeeklyMenuRepository;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.service.events.EventRegistrationService;
import com.tinyspring.garderie.service.events.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import com.tinyspring.garderie.repository.events.EventRatingRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParentPortalServiceImpl implements ParentPortalService {
    private static final Path AUTHORIZATION_UPLOAD_DIRECTORY =
            Paths.get("uploads", "event-authorizations").toAbsolutePath().normalize();

    private static final Set<RegistrationStatus> CAPACITY_CONSUMING_STATUSES =
            EnumSet.of(RegistrationStatus.CONFIRMED, RegistrationStatus.ATTENDED);
    private final EventRatingRepository eventRatingRepository;

    private final UserRepository userRepository;
    private final ChildRepository childRepository;
    private final ClasseRepository classeRepository;
    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final EventService eventService;
    private final EventRegistrationService eventRegistrationService;
    private final EventMapper eventMapper;
    private final EventRegistrationMapper eventRegistrationMapper;
    private final WeeklyMenuRepository weeklyMenuRepository;
    private final WeeklyMenuMapper weeklyMenuMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ParentChildResponse> getChildren(Long parentId) {
        User parent = getValidatedParent(parentId);

        return childRepository.findByParentIdOrderByFirstNameAscLastNameAsc(parent.getId()).stream()
                .map(this::toParentChildResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getEvents(Long parentId) {
        List<Child> children = getChildrenForParent(parentId);
        if (children.isEmpty()) {
            return List.of();
        }

        Set<Long> classroomIds = children.stream()
                .map(child -> child.getClassroom().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Long> childIds = children.stream()
                .map(Child::getId)
                .collect(Collectors.toSet());

        Set<Long> attendedCompletedEventIds = eventRegistrationRepository
                .findByChildIdInAndStatus(childIds, RegistrationStatus.ATTENDED).stream()
                .map(EventRegistration::getEventId)
                .collect(Collectors.toSet());

        List<Event> publishedEvents = eventRepository.findPublishedCandidatesForParent(
                        classroomIds,
                        EventStatus.PUBLISHED
                ).stream()
                .map(event -> eventService.getById(event.getId()))
                .filter(event -> {
                    EventResponse response = toParentEventResponse(event, childIds);
                    return isVisibleForChildren(response, classroomIds);
                })
                .toList();

        List<Event> completedEvents = attendedCompletedEventIds.isEmpty()
                ? List.of()
                : eventRepository.findCompletedAttendedEventsForParent(
                        attendedCompletedEventIds,
                        EventStatus.COMPLETED
                ).stream()
                .map(event -> eventService.getById(event.getId()))
                .toList();

        return java.util.stream.Stream.concat(
                        publishedEvents.stream(),
                        completedEvents.stream()
                )
                .distinct()
                .map(event -> toParentEventResponse(event, childIds))
                .toList();
    }

    private boolean isEventVisibleForParent(Event event,
                                            Set<Long> classroomIds,
                                            Set<Long> attendedCompletedEventIds,
                                            Set<Long> childIds) {
        if (event.getStatus() == EventStatus.PUBLISHED) {
            EventResponse response = toParentEventResponse(event, childIds);
            return isVisibleForChildren(response, classroomIds);
        }

        if (event.getStatus() == EventStatus.COMPLETED) {
            return attendedCompletedEventIds.contains(event.getId());
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventRegistrationResponse> getParticipations(Long parentId) {
        getValidatedParent(parentId);

        return eventRegistrationRepository.findByParentIdOrderByRegisteredAtDesc(parentId).stream()
                .map(this::toParentParticipationResponse)
                .toList();
    }

    @Override
    @Transactional
    public EventRegistrationResponse participate(Long parentId,
                                                 Long eventId,
                                                 Long childId,
                                                 String notes,
                                                 MultipartFile authorizationFile) {
        User parent = getValidatedParent(parentId);
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new ResourceNotFoundException("Enfant introuvable avec l'id : " + childId));

        if (!child.getParent().getId().equals(parent.getId())) {
            throw new InvalidStatusTransitionException(
                    "Cet enfant n'est pas rattache au parent connecte"
            );
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evenement introuvable avec l'id : " + eventId));

        if (!isChildEligibleForEvent(child, event)) {
            throw new InvalidStatusTransitionException(
                    "Cet evenement n'est pas accessible pour la classe de l'enfant selectionne"
            );
        }

        EventRegistration existingRegistration = eventRegistrationRepository
                .findByEventIdAndChildId(eventId, childId)
                .orElse(null);

        if (existingRegistration != null) {

            if (existingRegistration.getStatus() == RegistrationStatus.CANCELLED) {

                EventResponse eventResponse = toParentEventResponse(event, Set.of(childId));

                if (eventResponse.isFull()) {
                    existingRegistration.setStatus(RegistrationStatus.WAITLISTED);
                } else {
                    existingRegistration.setStatus(RegistrationStatus.CONFIRMED);
                }

                existingRegistration.setRegisteredAt(LocalDateTime.now());
                existingRegistration.setNotes(StringUtils.hasText(notes) ? notes.trim() : null);

                String authorizationDocUrl = storeAuthorizationPdf(authorizationFile);
                existingRegistration.setAuthorizationSigned(authorizationDocUrl != null);
                existingRegistration.setAuthorizationDocUrl(authorizationDocUrl);

                EventRegistration saved = eventRegistrationRepository.save(existingRegistration);
                return toParentParticipationResponse(saved);
            }

            throw new InvalidStatusTransitionException(
                    "Cet enfant possede deja une participation active pour cet evenement"
            );
        }

        String authorizationDocUrl = storeAuthorizationPdf(authorizationFile);
        EventRegistrationRequest request = EventRegistrationRequest.builder()
                .childId(childId)
                .parentId(parent.getId())
                .authorizationSigned(authorizationDocUrl != null)
                .authorizationDocUrl(authorizationDocUrl)
                .notes(StringUtils.hasText(notes) ? notes.trim() : null)
                .build();

        EventRegistration saved = eventRegistrationService.register(eventId, request);
        return toParentParticipationResponse(saved);
    }

    @Override
    @Transactional
    public EventRegistrationResponse cancelParticipation(Long parentId, Long registrationId) {
        User parent = getValidatedParent(parentId);
        EventRegistration registration = eventRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Participation introuvable avec l'id : " + registrationId));

        if (!registration.getParentId().equals(parent.getId())) {
            throw new InvalidStatusTransitionException("Cette participation n'appartient pas au parent connecte");
        }

        Event event = eventRepository.findById(registration.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Evenement introuvable avec l'id : " + registration.getEventId()));

        if (registration.getStatus() != RegistrationStatus.CONFIRMED) {
            throw new InvalidStatusTransitionException(
                    "Seules les participations confirmees peuvent etre annulees par le parent"
            );
        }

        if (event.getStartDatetime() == null || !event.getStartDatetime().isAfter(LocalDateTime.now().plusHours(24))) {
            throw new InvalidStatusTransitionException(
                    "L'annulation parentale n'est possible que plus de 24 heures avant le debut de l'evenement"
            );
        }

        EventRegistration cancelled = eventRegistrationService.cancel(registrationId);
        return toParentParticipationResponse(cancelled);
    }

    private User getValidatedParent(Long parentId) {
        User parent = userRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent introuvable avec l'id : " + parentId));

        if (parent.getRole() == null || parent.getRole().getName() != RoleName.PARENT) {
            throw new InvalidStatusTransitionException("L'utilisateur fourni n'a pas le role PARENT");
        }

        return parent;
    }

    private List<Child> getChildrenForParent(Long parentId) {
        User parent = getValidatedParent(parentId);
        return childRepository.findByParentIdOrderByFirstNameAscLastNameAsc(parent.getId());
    }

    private ParentChildResponse toParentChildResponse(Child child) {
        return ParentChildResponse.builder()
                .id(child.getId())
                .firstName(child.getFirstName())
                .lastName(child.getLastName())
                .classroomId(child.getClassroom().getId())
                .classroomName(child.getClassroom().getNom())
                .build();
    }

    private EventResponse toParentEventResponse(Event event, Set<Long> childIds) {
        EventResponse response = eventMapper.toResponse(event);

        long confirmedRegistrations = eventRegistrationRepository.countByEventIdAndStatusIn(
                event.getId(),
                CAPACITY_CONSUMING_STATUSES
        );
        long waitlistedRegistrations = eventRegistrationRepository.countByEventIdAndStatus(
                event.getId(),
                RegistrationStatus.WAITLISTED
        );

        Integer remainingCapacity = event.getMaxCapacity() == null
                ? null
                : Math.max(event.getMaxCapacity() - Math.toIntExact(confirmedRegistrations), 0);

        response.setConfirmedRegistrations(confirmedRegistrations);
        response.setWaitlistedRegistrations(waitlistedRegistrations);
        response.setRemainingCapacity(remainingCapacity);
        response.setFull(remainingCapacity != null && remainingCapacity == 0);
        response.setRegistrationOpen(!response.isFull());

        if (event.getClassroomId() != null) {
            classeRepository.findById(event.getClassroomId())
                    .map(Classe::getNom)
                    .ifPresent(response::setClassroomName);
        }

        response.setTargetedClassroomNames(resolveClassroomNames(response.getTargetClassroomIds()));

        // =========================
        //  PARTIE RATING
        // =========================

        List<EventRating> ratings = eventRatingRepository.findByEventId(event.getId());

        response.setRatingCount((long) ratings.size());

        double average = ratings.stream()
                .mapToInt(EventRating::getStars)
                .average()
                .orElse(0.0);

        response.setAverageRating(average);

        List<Long> attendedChildIds = eventRegistrationRepository
                .findByChildIdInAndStatus(childIds, RegistrationStatus.ATTENDED)
                .stream()
                .filter(reg -> reg.getEventId().equals(event.getId()))
                .map(EventRegistration::getChildId)
                .distinct()
                .toList();

        Long rateableChildId = null;
        Integer myRating = null;

        if (!attendedChildIds.isEmpty()) {
            List<EventRating> childRatings = eventRatingRepository.findByEventIdAndChildIdIn(event.getId(), attendedChildIds);

            if (!childRatings.isEmpty()) {
                EventRating existingRating = childRatings.get(0);
                rateableChildId = existingRating.getChildId();
                myRating = existingRating.getStars();
            } else {
                rateableChildId = attendedChildIds.get(0);
            }
        }

        boolean canRate = event.getStatus() == EventStatus.COMPLETED && rateableChildId != null;

        response.setRateable(canRate);
        response.setRateableChildId(rateableChildId);
        response.setMyRating(myRating);

        return response;
    }

    private boolean isVisibleForChildren(EventResponse event, Set<Long> classroomIds) {
        Set<Long> eventScope = new LinkedHashSet<>();

        if (event.getClassroomId() != null) {
            eventScope.add(event.getClassroomId());
        }
        if (event.getTargetClassroomIds() != null) {
            eventScope.addAll(event.getTargetClassroomIds());
        }

        if (eventScope.isEmpty()) {
            return false;
        }

        return classroomIds.stream().anyMatch(eventScope::contains);
    }

    private boolean isChildEligibleForEvent(Child child, Event event) {
        Set<Long> eventScope = new LinkedHashSet<>();
        if (event.getClassroomId() != null) {
            eventScope.add(event.getClassroomId());
        }
        eventScope.addAll(parseTargetClassroomIds(event.getTargetClassroomIds()));
        return eventScope.contains(child.getClassroom().getId());
    }

    private List<String> resolveClassroomNames(Collection<Long> classroomIds) {
        if (classroomIds == null || classroomIds.isEmpty()) {
            return List.of();
        }

        Map<Long, String> byId = classeRepository.findAllById(classroomIds).stream()
                .collect(Collectors.toMap(Classe::getId, Classe::getNom));

        return classroomIds.stream()
                .map(byId::get)
                .filter(StringUtils::hasText)
                .toList();
    }

    private List<Long> parseTargetClassroomIds(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }

        return List.of(raw.split(",")).stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .distinct()
                .toList();
    }

    private String storeAuthorizationPdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = extractExtension(originalFilename).toLowerCase();
        if (!".pdf".equals(extension)) {
            throw new InvalidStatusTransitionException("Le document d'autorisation doit etre un fichier PDF");
        }

        try {
            Files.createDirectories(AUTHORIZATION_UPLOAD_DIRECTORY);

            String uniqueFilename = UUID.randomUUID() + extension;
            Path targetFile = AUTHORIZATION_UPLOAD_DIRECTORY.resolve(uniqueFilename).normalize();
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/event-authorizations/" + uniqueFilename;
        } catch (IOException exception) {
            throw new InvalidStatusTransitionException(
                    "Impossible d'enregistrer le document d'autorisation parentale"
            );
        }
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }

        return filename.substring(filename.lastIndexOf('.'));
    }

    private EventRegistrationResponse toParentParticipationResponse(EventRegistration registration) {
        EventRegistrationResponse response = eventRegistrationMapper.toResponse(registration);
        Event event = resolveEvent(registration);
        response.setEventTitle(resolveEventTitle(registration));
        response.setChildFullName(resolveChildFullName(registration));
        response.setEventStartDatetime(event != null ? event.getStartDatetime() : null);
        response.setCancellableByParent(isCancellableByParent(registration, event));
        return response;
    }

    private Event resolveEvent(EventRegistration registration) {
        if (registration.getEvent() != null) {
            return registration.getEvent();
        }

        return eventRepository.findById(registration.getEventId()).orElse(null);
    }

    private boolean isCancellableByParent(EventRegistration registration, Event event) {
        return registration.getStatus() == RegistrationStatus.CONFIRMED
                && event != null
                && event.getStartDatetime() != null
                && event.getStartDatetime().isAfter(LocalDateTime.now().plusHours(24));
    }

    private String resolveEventTitle(EventRegistration registration) {
        if (registration.getEvent() != null && StringUtils.hasText(registration.getEvent().getTitle())) {
            return registration.getEvent().getTitle();
        }

        return eventRepository.findById(registration.getEventId())
                .map(Event::getTitle)
                .orElse("Evenement");
    }

    private String resolveChildFullName(EventRegistration registration) {
        if (registration.getChild() != null) {
            return (registration.getChild().getFirstName() + " " + registration.getChild().getLastName()).trim();
        }

        return childRepository.findById(registration.getChildId())
                .map(child -> (child.getFirstName() + " " + child.getLastName()).trim())
                .orElse("Enfant");
    }

    @Override
    @Transactional
    public EventRatingResponse rateEvent(Long parentId, Long eventId, EventRatingRequest request) {
        User parent = getValidatedParent(parentId);

        Child child = childRepository.findById(request.getChildId())
                .orElseThrow(() -> new ResourceNotFoundException("Enfant introuvable avec l'id : " + request.getChildId()));

        if (!child.getParent().getId().equals(parent.getId())) {
            throw new InvalidStatusTransitionException("Cet enfant n'est pas rattache au parent connecte");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evenement introuvable avec l'id : " + eventId));

        if (event.getStatus() != EventStatus.COMPLETED) {
            throw new InvalidStatusTransitionException("Seuls les evenements termines peuvent etre notes");
        }

        boolean attended = eventRegistrationRepository.findByChildIdInAndStatus(
                        Set.of(child.getId()),
                        RegistrationStatus.ATTENDED
                ).stream()
                .anyMatch(registration -> registration.getEventId().equals(eventId));

        if (!attended) {
            throw new InvalidStatusTransitionException(
                    "Le parent ne peut noter qu'un evenement auquel son enfant a effectivement participe"
            );
        }

        EventRating rating = eventRatingRepository.findByEventIdAndChildId(eventId, child.getId())
                .orElse(
                        EventRating.builder()
                                .eventId(eventId)
                                .childId(child.getId())
                                .parentId(parentId)
                                .createdAt(LocalDateTime.now())
                                .build()
                );

        rating.setStars(request.getStars());
        rating.setComment(request.getComment() != null ? request.getComment().trim() : null);
        rating.setUpdatedAt(LocalDateTime.now());

        EventRating saved = eventRatingRepository.save(rating);

        return EventRatingResponse.builder()
                .id(saved.getId())
                .eventId(saved.getEventId())
                .childId(saved.getChildId())
                .parentId(saved.getParentId())
                .stars(saved.getStars())
                .comment(saved.getComment())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public List<WeeklyMenuResponse> getMenus(Long parentId) {
        User parent = getValidatedParent(parentId);

        List<Child> children = childRepository.findByParentIdOrderByFirstNameAscLastNameAsc(parent.getId());

        List<WeeklyMenu> menus = weeklyMenuRepository
                .findByStatusOrderByWeekStartDateDesc(MenuStatus.PUBLISHED);

        return menus.stream()
                .map(menu -> {
                    WeeklyMenuResponse response = weeklyMenuMapper.toResponse(menu);
                    enrichWithAllergenConflicts(response, menu, children);
                    return response;
                })
                .toList();
    }

    private void enrichWithAllergenConflicts(
            WeeklyMenuResponse response,
            WeeklyMenu menu,
            List<Child> children
    ) {
        if (response.getDailyMenus() == null || menu.getDailyMenus() == null) {
            return;
        }

        for (DailyMenuResponse dailyResponse : response.getDailyMenus()) {
            DailyMenu dailyEntity = menu.getDailyMenus()
                    .stream()
                    .filter(day -> day.getId() != null && day.getId().equals(dailyResponse.getId()))
                    .findFirst()
                    .orElse(null);

            if (dailyEntity == null) {
                dailyResponse.setAllergenConflictFlags(List.of());
                dailyResponse.setAllergenConflictMessages(List.of());
                continue;
            }

            List<String> flags = new ArrayList<>();
            List<String> messages = new ArrayList<>();

            for (Dish dish : dailyEntity.getDishes()) {
                Set<String> dishAllergens = normalizeToSet(dish.getAllergens());

                if (dishAllergens.isEmpty()) {
                    continue;
                }

                for (Child child : children) {
                    Set<String> childAllergies = normalizeToSet(child.getAllergies());

                    for (String dishAllergen : dishAllergens) {
                        if (childAllergies.contains(dishAllergen)) {
                            String childFullName = child.getFirstName() + " " + child.getLastName();
                            String allergenLabel = capitalize(dishAllergen);

                            flags.add(allergenLabel + " — " + child.getFirstName());

                            messages.add(
                                    dish.getName()
                                            + " contient du "
                                            + dishAllergen
                                            + " — "
                                            + childFullName
                                            + " est allergique."
                            );
                        }
                    }
                }
            }

            dailyResponse.setAllergenConflictFlags(flags.stream().distinct().toList());
            dailyResponse.setAllergenConflictMessages(messages.stream().distinct().toList());
        }
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
        if (value == null) {
            return "";
        }

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
