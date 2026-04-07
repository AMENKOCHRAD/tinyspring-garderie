package com.tinyspring.garderie.dto.Events;

import com.tinyspring.garderie.entity.Events.EventStatus;
import com.tinyspring.garderie.entity.Events.EventType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class EventRequest {
    @Size(max = 255, message = "Le titre ne doit pas depasser 255 caracteres")
    private String title;

    @Size(max = 5000, message = "La description est trop longue")
    private String description;

    @NotNull(message = "Le type d'evenement est obligatoire")
    private EventType type;

    private EventStatus status;

    @NotNull(message = "La date de debut est obligatoire")
    private LocalDateTime startDatetime;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDateTime endDatetime;

    @Size(max = 255, message = "Le lieu ne doit pas depasser 255 caracteres")
    private String location;

    private Double latitude;
    private Double longitude;

    @Min(value = 1, message = "La capacite maximale doit etre superieure a 0")
    private Integer maxCapacity;

    @NotNull(message = "Le champ requiresAuthorization est obligatoire")
    private Boolean requiresAuthorization;

    @NotNull(message = "La classe ciblee est obligatoire")
    private Long classroomId;

    private List<Long> targetClassroomIds;

    @NotNull(message = "Le createur est obligatoire")
    private Long createdBy;

    @Min(value = 0, message = "Le prix doit etre positif")
    private Double eventPrice;

    @Size(max = 500, message = "URL photo trop longue")
    private String photoEvent;
}
