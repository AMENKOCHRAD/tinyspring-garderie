package com.tinyspring.garderie.dto.Events;

import com.tinyspring.garderie.entity.Events.EventStatus;
import com.tinyspring.garderie.entity.Events.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {
    private Long id;
    private String title;
    private String description;
    private EventType type;
    private EventStatus status;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private String location;
    private Double latitude;
    private Double longitude;
    private Integer maxCapacity;
    private boolean requiresAuthorization;
    private Long classroomId;
    private String classroomName;
    private List<Long> targetClassroomIds;
    private List<String> targetedClassroomNames;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double eventPrice;
    private String photoEvent;
    private Long confirmedRegistrations;
    private Long waitlistedRegistrations;
    private Integer remainingCapacity;
    private boolean full;
    private boolean registrationOpen;
    private boolean rateable;
    private Long rateableChildId;
    private Integer myRating;
    private Double averageRating;
    private Long ratingCount;
}
