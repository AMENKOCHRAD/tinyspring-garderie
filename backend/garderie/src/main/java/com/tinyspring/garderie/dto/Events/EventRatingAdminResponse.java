package com.tinyspring.garderie.dto.Events;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EventRatingAdminResponse {
    private Long id;
    private Long eventId;
    private Long childId;
    private String childFullName;
    private Integer stars;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
