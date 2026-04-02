package com.tinyspring.garderie.entity;

import jakarta.persistence.*;
import lombok.*;
import com.tinyspring.garderie.entity.enums.ReclamationPriority;
import com.tinyspring.garderie.entity.enums.ReclamationStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "reclamation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reclamation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    private ReclamationStatus status;

    @Enumerated(EnumType.STRING)
    private ReclamationPriority priority;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "parent_id", nullable = false)
    private User parent;

    @ManyToOne
    @JoinColumn(name = "assigned_admin_id")
    private User assignedAdmin;

    @OneToOne
    @JoinColumn(name = "conversation_id", unique = true)
    private Conversation conversation;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = ReclamationStatus.OPEN;
        }

        if (this.priority == null) {
            this.priority = ReclamationPriority.MEDIUM;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
