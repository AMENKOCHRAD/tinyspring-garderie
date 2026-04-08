package com.tinyspring.garderie.entity.Events;

import com.tinyspring.garderie.entity.Classes.Classe;
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

    private Double latitude;
    private Double longitude;

    private Integer maxCapacity;

    private boolean requiresAuthorization;

    @Column(name = "event_price")
    private Double eventPrice;

    @Column(name = "photo_event", length = 500)
    private String photoEvent;

    @Column(name = "classroom_id")
    private Long classroomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "classroom_id",
            insertable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_event_classe")
    )
    private Classe classroom;
    @Column(name = "target_classroom_ids", columnDefinition = "TEXT")
    private String targetClassroomIds;
    private Long createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
