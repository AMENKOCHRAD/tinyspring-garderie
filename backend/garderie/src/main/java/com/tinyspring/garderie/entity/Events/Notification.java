package com.tinyspring.garderie.entity.Events;

import com.tinyspring.garderie.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "NotificationEvents")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 3000)
    private String message;

    private String type;

    @Builder.Default
    private boolean seen = false;

    private LocalDateTime createdAt;

    @Builder.Default
    private String priority = "NORMAL";

    private Long relatedEntityId;

    private String relatedEntityType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private User parent;
}