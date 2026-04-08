package com.tinyspring.garderie.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateReclamationStatusRequest {

    // OPEN, IN_PROGRESS, RESOLVED, REJECTED
    private String status;
}