package com.tinyspring.garderie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildMatchingRequest {
    private Integer age;
    private String languePrincipale;
}
