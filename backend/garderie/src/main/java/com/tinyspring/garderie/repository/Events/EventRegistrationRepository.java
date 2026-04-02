package com.tinyspring.garderie.repository.Events;

import com.tinyspring.garderie.entity.Events.EventRegistration;
import com.tinyspring.garderie.entity.Events.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRegistrationRepository  extends JpaRepository<EventRegistration, Long> {
    List<EventRegistration> findByEventIdOrderByRegisteredAt(Long eventId);

    Optional<EventRegistration> findFirstByEventIdAndStatusOrderByRegisteredAt(
            Long eventId,
            RegistrationStatus status
    );

    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);
}
