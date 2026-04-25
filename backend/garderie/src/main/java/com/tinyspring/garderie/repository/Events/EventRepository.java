package com.tinyspring.garderie.repository.Events;

import com.tinyspring.garderie.entity.Events.Event;
import com.tinyspring.garderie.entity.Events.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface EventRepository extends JpaRepository<Event, Long>  {


    @Query("""
        SELECT e
        FROM Event e
        WHERE e.status = :status
        AND (
            e.classroomId IN :classroomIds
            OR e.targetClassroomIds IS NOT NULL
        )
        ORDER BY e.startDatetime ASC
    """)
    List<Event> findPublishedCandidatesForParent(
            @Param("classroomIds") Collection<Long> classroomIds,
            @Param("status") EventStatus status
    );

    @Query("""
        SELECT e
        FROM Event e
        WHERE e.status = :status
        AND e.id IN :eventIds
        ORDER BY e.startDatetime DESC
    """)
    List<Event> findCompletedAttendedEventsForParent(
            @Param("eventIds") Set<Long> eventIds,
            @Param("status") EventStatus status
    );

    List<Event> findByStatusOrderByStartDatetimeAsc(EventStatus status);
}
