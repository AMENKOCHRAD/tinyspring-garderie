package com.tinyspring.garderie.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateConversationStatusRequest {

    // OPEN, CLOSED, ARCHIVED
    private String status;
}