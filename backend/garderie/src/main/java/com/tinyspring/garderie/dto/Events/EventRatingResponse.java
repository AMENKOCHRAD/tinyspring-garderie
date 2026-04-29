package com.tinyspring.garderie.dto.Events;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EventRatingResponse {
    private Long id;
    private Long eventId;
    private Long childId;
    private Long parentId;
    private Integer stars;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
