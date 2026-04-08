package com.tinyspring.garderie.entity;

import jakarta.persistence.*;
import lombok.*;
import com.tinyspring.garderie.entity.enums.ConversationStatus;
import com.tinyspring.garderie.entity.enums.ConversationType;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "conversation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subject;

    @Enumerated(EnumType.STRING)
    private ConversationType type;

    @Enumerated(EnumType.STRING)
    private ConversationStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "parent_id", nullable = false)
    private User parent;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private User admin;

    @ManyToOne
    @JoinColumn(name = "animatrice_id")
    private User animatrice;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ConversationStatus.OPEN;
        }
        if (this.type == null) {
            this.type = ConversationType.NORMAL;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}