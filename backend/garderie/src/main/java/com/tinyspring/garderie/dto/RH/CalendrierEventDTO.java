package com.tinyspring.garderie.dto.RH;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendrierEventDTO {
    private String id;
    private String title;
    private String start;
    private String end;
    private String color;
    private String type; // ABSENCE ou FORMATION
}