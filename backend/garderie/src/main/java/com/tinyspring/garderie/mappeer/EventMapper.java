package com.tinyspring.garderie.mappeer;

import com.tinyspring.garderie.dto.Events.EventRequest;
import com.tinyspring.garderie.dto.Events.EventResponse;
import com.tinyspring.garderie.entity.events.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface EventMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "eventPrice", source = "eventPrice")
    @Mapping(target = "targetClassroomIds", expression = "java(serializeClassroomIds(request.getTargetClassroomIds()))")
    Event toEntity(EventRequest request);

    @Mapping(target = "eventPrice", source = "eventPrice")
    @Mapping(target = "classroomName", expression = "java(event.getClassroom() != null ? event.getClassroom().getNiveau() : null)")
    @Mapping(target = "targetClassroomIds", expression = "java(deserializeClassroomIds(event.getTargetClassroomIds()))")
    EventResponse toResponse(Event event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "eventPrice", source = "eventPrice")
    @Mapping(target = "targetClassroomIds", expression = "java(serializeClassroomIds(request.getTargetClassroomIds()))")
    void updateEntityFromRequest(EventRequest request, @MappingTarget Event event);

    default String serializeClassroomIds(List<Long> classroomIds) {
        if (classroomIds == null || classroomIds.isEmpty()) {
            return null;
        }

        return classroomIds.stream()
                .filter(id -> id != null)
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    default List<Long> deserializeClassroomIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(segment -> !segment.isEmpty())
                .map(Long::valueOf)
                .toList();
    }
}
