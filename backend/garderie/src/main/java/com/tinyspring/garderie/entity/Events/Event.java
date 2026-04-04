package com.tinyspring.garderie.entity.Events;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "event")
@Getter
@Setter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private EventType type;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;

    private String location;

    private Integer maxCapacity;

    private boolean requiresAuthorization;

    @Column(name = "event_price")
    private Double eventPrice;

    @Column(name = "photo_event", length = 500)
    private String photoEvent;

    private Long classroomId;
    private Long createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
