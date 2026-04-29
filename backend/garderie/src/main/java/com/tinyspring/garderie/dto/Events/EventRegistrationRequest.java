package com.tinyspring.garderie.dto.Events;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRegistrationRequest {

    @NotNull(message = "L'identifiant de l'enfant est obligatoire")
    private Long childId;

    @NotNull(message = "L'identifiant du parent est obligatoire")
    private Long parentId;

    @NotNull(message = "Le champ authorizationSigned est obligatoire")
    private Boolean authorizationSigned;

    @Size(max = 500, message = "L'URL du document ne doit pas dépasser 500 caractères")
    private String authorizationDocUrl;

    @Size(max = 5000, message = "Les notes sont trop longues")
    private String notes;
}
