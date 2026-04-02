package com.tinyspring.garderie.entity.Events;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_registration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "child_id", nullable = false)
    private Long childId;

    @Column(name = "parent_id", nullable = false)
    private Long parentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus status;

    @Column(name = "authorization_signed", nullable = false)
    private boolean authorizationSigned;

    @Column(name = "authorization_doc_url", length = 500)
    private String authorizationDocUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "registered_at", nullable = false, updatable = false)
    private LocalDateTime registeredAt;
}
