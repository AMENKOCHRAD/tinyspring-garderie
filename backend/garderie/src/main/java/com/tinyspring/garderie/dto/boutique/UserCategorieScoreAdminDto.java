package com.tinyspring.garderie.dto.boutique;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UserCategorieScoreAdminDto {
    private Long id;
    private Long userId;
    private String userNom;
    private String userEmail;
    private Long categorieId;
    private String categorieNom;
    private Double score;
    private Integer nbInteractions;
    private LocalDateTime derniereInteraction;
}