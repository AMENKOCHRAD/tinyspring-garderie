package com.tinyspring.garderie.repository.Events;

import com.tinyspring.garderie.entity.Events.Event;
import com.tinyspring.garderie.entity.Events.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByClassroomIdAndStatus(Long classroomId, EventStatus status);

    List<Event> findByStatusOrderByStartDatetimeAsc(EventStatus status);
}
