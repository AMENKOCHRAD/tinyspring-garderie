package com.tinyspring.garderie.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateReclamationRequest {

    private String title;
    private String description;
    private String priority;
    private String category;
    private String adminComment;
}