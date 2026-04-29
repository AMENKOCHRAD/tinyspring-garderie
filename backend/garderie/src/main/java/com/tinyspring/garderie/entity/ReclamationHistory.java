package com.tinyspring.garderie.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tinyspring.garderie.entity.enums.ReclamationHistoryActionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reclamation_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReclamationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ReclamationHistoryActionType actionType;

    @Column(nullable = false, length = 255)
    private String actionLabel;

    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    @Column(nullable = false, length = 150)
    private String actorName;

    @Column(nullable = false, length = 50)
    private String actorRole;

    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reclamation_id", nullable = false)
    @JsonIgnore
    private Reclamation reclamation;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}