package com.tinyspring.garderie.mappeer;

import com.tinyspring.garderie.dto.Events.EventRequest;
import com.tinyspring.garderie.dto.Events.EventResponse;
import com.tinyspring.garderie.entity.Events.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EventMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "eventPrice", source = "eventPrice")
    Event toEntity(EventRequest request);

    @Mapping(target = "eventPrice", source = "eventPrice")
    EventResponse toResponse(Event event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "eventPrice", source = "eventPrice")
    void updateEntityFromRequest(EventRequest request, @MappingTarget Event event);
}
