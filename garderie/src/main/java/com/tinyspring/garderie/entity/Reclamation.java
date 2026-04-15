package com.tinyspring.garderie.entity;

import com.tinyspring.garderie.entity.enums.ReclamationCategory;
import com.tinyspring.garderie.entity.enums.ReclamationPriority;
import com.tinyspring.garderie.entity.enums.ReclamationStatus;
import jakarta.persistence.*;
import lombok.*;

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

    @Enumerated(EnumType.STRING)
    private ReclamationCategory category;

    @Enumerated(EnumType.STRING)
    private ReclamationCategory predictedCategory;

    private Double classificationConfidence;

    private Boolean autoClassified;

    @Column(columnDefinition = "TEXT")
    private String adminComment;

    private String imageName;
    private String imagePath;

    private String attachmentName;
    private String attachmentPath;
    private String attachmentType;

    @Column(name = "predicted_priority")
    @Enumerated(EnumType.STRING)
    private ReclamationPriority predictedPriority;

    @Column(name = "priority_confidence")
    private Double priorityConfidence;

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

        if (this.category == null) {
            this.category = ReclamationCategory.AUTRE;
        }

        if (this.autoClassified == null) {
            this.autoClassified = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}