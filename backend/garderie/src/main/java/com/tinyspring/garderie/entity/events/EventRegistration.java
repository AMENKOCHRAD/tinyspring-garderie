package com.tinyspring.garderie.entity.events;

import com.tinyspring.garderie.entity.Children.Child;
import com.tinyspring.garderie.entity.User;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "event_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_event_registration_event")
    )
    private Event event;

    @Column(name = "child_id", nullable = false)
    private Long childId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "child_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_event_registration_child")
    )
    private Child child;

    @Column(name = "parent_id", nullable = false)
    private Long parentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "parent_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_event_registration_parent")
    )
    private User parent;

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
