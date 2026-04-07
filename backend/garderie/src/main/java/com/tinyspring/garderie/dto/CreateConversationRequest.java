package com.tinyspring.garderie.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateConversationRequest {

    private String subject;

    private Long receiverId;

    // "ADMIN" ou "ANIMATRICE"
    private String receiverRole;
}