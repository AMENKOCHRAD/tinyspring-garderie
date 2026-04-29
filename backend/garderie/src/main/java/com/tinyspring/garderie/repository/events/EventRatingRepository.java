package com.tinyspring.garderie.repository.events;

import com.tinyspring.garderie.entity.events.EventRating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
public interface EventRatingRepository extends JpaRepository<EventRating, Long>{
    Optional<EventRating> findByEventIdAndChildId(Long eventId, Long childId);

    List<EventRating> findByEventId(Long eventId);
    List<EventRating> findByEventIdAndChildIdIn(Long eventId, Collection<Long> childIds);
}
