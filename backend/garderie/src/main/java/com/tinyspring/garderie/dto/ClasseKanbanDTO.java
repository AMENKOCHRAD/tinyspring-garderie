package com.tinyspring.garderie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.tinyspring.garderie.entity.User;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClasseKanbanDTO {
    private Long id;
    private String nom;
    private Integer capaciteMax;
    private Integer countEnfantsActifs;
    private List<User> enfants;
}
