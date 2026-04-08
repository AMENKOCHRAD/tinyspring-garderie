package com.tinyspring.garderie.dto.RH;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
    private Long id;
    private String message;
    private String type;
    private boolean read;
    private LocalDateTime createdAt;
}