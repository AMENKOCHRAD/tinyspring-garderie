package com.tinyspring.garderie.dto.Events;
import com.tinyspring.garderie.entity.Events.EventStatus;
import com.tinyspring.garderie.entity.Events.EventType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {
    @Size(max = 255, message = "Le titre ne doit pas dépasser 255 caractères")
    private String title;

    @Size(max = 5000, message = "La description est trop longue")
    private String description;

    @NotNull(message = "Le type d'événement est obligatoire")
    private EventType type;

    private EventStatus status;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDateTime startDatetime;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDateTime endDatetime;

    @Size(max = 255, message = "Le lieu ne doit pas dépasser 255 caractères")
    private String location;

    @Min(value = 1, message = "La capacité maximale doit être supérieure à 0")
    private Integer maxCapacity;

    @NotNull(message = "Le champ requiresAuthorization est obligatoire")
    private Boolean  requiresAuthorization;

    @NotNull(message = "La classe ciblée est obligatoire")
    private Long classroomId;

    @NotNull(message = "Le créateur est obligatoire")
    private Long createdBy;

    @Min(value = 0, message = "Le prix doit être positif")
    private Double priceEvent;

    @Size(max = 500, message = "URL photo trop longue")
    private String photoEvent;
}
