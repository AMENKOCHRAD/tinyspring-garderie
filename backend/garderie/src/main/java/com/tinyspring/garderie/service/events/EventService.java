package com.tinyspring.garderie.service.events;

import com.tinyspring.garderie.dto.Events.EventRatingAdminResponse;
import com.tinyspring.garderie.dto.Events.EventRequest;
import com.tinyspring.garderie.dto.Events.EventResponse;
import com.tinyspring.garderie.entity.events.Event;
import com.tinyspring.garderie.entity.events.EventStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EventService {
    Event create(EventRequest request);

    List<Event> getAll();

    List<Event> getPublished();

    Event getById(Long id);

    Event getPublishedById(Long id);

    Event update(Long id, EventRequest request);

    void delete(Long id);

    Event publish(Long id);

    Event uploadPhoto(Long id, MultipartFile file);
     void validateLocation(Event event);
     int getStatusPriority(EventStatus status);
    List<EventResponse> getAllWithRatings();
    List<EventRatingAdminResponse> getRatingsForAdmin(Long eventId);


}
