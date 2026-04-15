package com.tinyspring.garderie.dto.RH;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RapportRHDTO {
    private Long id;
    private String question;
    private String typeRapport;
    private String periode;
    private String contenu;
    private LocalDateTime dateGeneration;
}