package com.tinyspring.garderie.dto.Events;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private String type;
    private boolean seen;
    private LocalDateTime createdAt;
}
