package com.tinyspring.garderie.mapper;

import com.tinyspring.garderie.dto.Events.EventRequest;
import com.tinyspring.garderie.dto.Events.EventResponse;
import com.tinyspring.garderie.entity.Classes.Classe;
import com.tinyspring.garderie.entity.events.Event;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventMapperTest {

    private final EventMapper mapper = Mappers.getMapper(EventMapper.class);

    @Test
    void serializeClassroomIds_shouldReturnNull_whenNullOrEmpty() {
        assertNull(mapper.serializeClassroomIds(null));
        assertNull(mapper.serializeClassroomIds(List.of()));
    }

    @Test
    void serializeClassroomIds_shouldRemoveNullsAndDuplicates() {
        List<Long> input = new java.util.ArrayList<>(
                java.util.Arrays.asList(1L, 2L, null, 2L, 3L)
        );

        String result = mapper.serializeClassroomIds(input);

        assertEquals("1,2,3", result);
    }

    @Test
    void deserializeClassroomIds_shouldReturnEmptyList_whenNullBlankOrEmptySegments() {
        assertTrue(mapper.deserializeClassroomIds(null).isEmpty());
        assertTrue(mapper.deserializeClassroomIds("   ").isEmpty());
        assertTrue(mapper.deserializeClassroomIds(" , , ").isEmpty());
    }

    @Test
    void deserializeClassroomIds_shouldConvertStringToLongList() {
        List<Long> result = mapper.deserializeClassroomIds("1, 2,3");

        assertEquals(List.of(1L, 2L, 3L), result);
    }

    @Test
    void toEntity_shouldMapEventRequest() {
        EventRequest request = new EventRequest();
        request.setTitle("Sortie");
        request.setDescription("Sortie éducative");
        request.setEventPrice(20.0);
        request.setTargetClassroomIds(List.of(1L, 2L, 2L));

        Event result = mapper.toEntity(request);

        assertNotNull(result);
        assertNull(result.getId());
        assertEquals("Sortie", result.getTitle());
        assertEquals("Sortie éducative", result.getDescription());
        assertEquals(20.0, result.getEventPrice());
        assertEquals("1,2", result.getTargetClassroomIds());
    }

    @Test
    void toResponse_shouldMapEventWithClassroomAndTargetClassrooms() {
        Classe classe = new Classe();
        classe.setNiveau("Moyenne section");

        Event event = new Event();
        event.setId(1L);
        event.setTitle("Fête");
        event.setEventPrice(15.0);
        event.setClassroom(classe);
        event.setTargetClassroomIds("5,6");

        EventResponse result = mapper.toResponse(event);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Fête", result.getTitle());
        assertEquals(15.0, result.getEventPrice());
        assertEquals("Moyenne section", result.getClassroomName());
        assertEquals(List.of(5L, 6L), result.getTargetClassroomIds());
    }

    @Test
    void toResponse_shouldReturnNullClassroomName_whenClassroomIsNull() {
        Event event = new Event();
        event.setTargetClassroomIds(null);

        EventResponse result = mapper.toResponse(event);

        assertNotNull(result);
        assertNull(result.getClassroomName());
        assertTrue(result.getTargetClassroomIds().isEmpty());
    }

    @Test
    void updateEntityFromRequest_shouldUpdateEventWithoutChangingId() {
        Event event = new Event();
        event.setId(99L);

        EventRequest request = new EventRequest();
        request.setTitle("Nouveau titre");
        request.setEventPrice(30.0);
        request.setTargetClassroomIds(List.of(7L, 8L));

        mapper.updateEntityFromRequest(request, event);

        assertEquals(99L, event.getId());
        assertEquals("Nouveau titre", event.getTitle());
        assertEquals(30.0, event.getEventPrice());
        assertEquals("7,8", event.getTargetClassroomIds());
    }
}