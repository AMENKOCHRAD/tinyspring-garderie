package com.tinyspring.garderie.service.Events;

import com.tinyspring.garderie.dto.Events.EventRequest;
import com.tinyspring.garderie.entity.Events.Event;

import java.util.List;

public interface EventService {
    Event create(EventRequest request);

    List<Event> getAll();

    Event getById(Long id);

    Event update(Long id, EventRequest request);

    void delete(Long id);

    Event publish(Long id);
}
