package com.tinyspring.garderie.mappeer;

import com.tinyspring.garderie.dto.Events.EventRegistrationResponse;
import com.tinyspring.garderie.entity.events.EventRegistration;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventRegistrationMapper {
    EventRegistrationResponse toResponse(EventRegistration registration);
}
