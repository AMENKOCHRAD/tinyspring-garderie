package com.tinyspring.garderie.mapper;

import com.tinyspring.garderie.dto.Events.EventRegistrationResponse;
import com.tinyspring.garderie.entity.events.EventRegistration;
import com.tinyspring.garderie.entity.events.RegistrationStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

class EventRegistrationMapperTest {

    private final EventRegistrationMapper mapper =
            Mappers.getMapper(EventRegistrationMapper.class);

    @Test
    void toResponse_shouldMapEntityToResponse() {
        EventRegistration registration = new EventRegistration();
        registration.setId(1L);
        registration.setEventId(10L);
        registration.setChildId(20L);
        registration.setParentId(30L);
        registration.setStatus(RegistrationStatus.CONFIRMED);

        EventRegistrationResponse result = mapper.toResponse(registration);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(10L, result.getEventId());
        assertEquals(20L, result.getChildId());
        assertEquals(30L, result.getParentId());
        assertEquals(RegistrationStatus.CONFIRMED, result.getStatus());
    }
}