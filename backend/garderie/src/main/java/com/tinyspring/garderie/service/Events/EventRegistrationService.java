package com.tinyspring.garderie.service.Events;

import com.tinyspring.garderie.dto.Events.EventRegistrationRequest;
import com.tinyspring.garderie.entity.Events.EventRegistration;

import java.util.List;

public interface EventRegistrationService {
    EventRegistration register(Long eventId, EventRegistrationRequest request);

    List<EventRegistration> getByEventId(Long eventId);

    EventRegistration confirm(Long registrationId);

    EventRegistration cancel(Long registrationId);

    EventRegistration markAttended(Long registrationId);

    EventRegistration markAbsent(Long registrationId);
}
