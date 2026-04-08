package com.tinyspring.garderie.repository.Events;

import com.tinyspring.garderie.entity.Events.EventRegistration;
import com.tinyspring.garderie.entity.Events.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface EventRegistrationRepository  extends JpaRepository<EventRegistration, Long> {
    List<EventRegistration> findByEventIdOrderByRegisteredAtAsc(Long eventId);
    List<EventRegistration> findByParentIdOrderByRegisteredAtDesc(Long parentId);

    Optional<EventRegistration> findFirstByEventIdAndStatusOrderByRegisteredAtAsc(
            Long eventId,
            RegistrationStatus status
    );

    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);

    long countByEventIdAndStatusIn(Long eventId, Collection<RegistrationStatus> statuses);

    boolean existsByEventIdAndChildIdAndStatusIn(Long eventId, Long childId, Collection<RegistrationStatus> statuses);

    boolean existsByEventIdAndChildId(Long eventId, Long childId);
}
