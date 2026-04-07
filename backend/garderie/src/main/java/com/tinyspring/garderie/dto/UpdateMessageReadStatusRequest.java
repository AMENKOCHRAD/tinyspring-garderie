package com.tinyspring.garderie.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMessageReadStatusRequest {

    private Boolean isRead;
}