package com.tinyspring.garderie.dto.Classes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClasseOptionResponse {
    private Long id;
    private String nom;
    private String niveau;
}
