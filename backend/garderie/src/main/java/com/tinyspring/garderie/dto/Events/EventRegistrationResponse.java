package com.tinyspring.garderie.dto.Events;

import com.tinyspring.garderie.entity.Events.RegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRegistrationResponse {
    private Long id;
    private Long eventId;
    private Long childId;
    private Long parentId;
    private RegistrationStatus status;
    private boolean authorizationSigned;
    private String authorizationDocUrl;
    private String notes;
    private LocalDateTime registeredAt;


}
