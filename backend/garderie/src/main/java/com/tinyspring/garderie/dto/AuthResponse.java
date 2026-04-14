package com.tinyspring.garderie.dto;

public record AuthResponse(
        String message,
        Long id,
        String nom,
        String email,
        String role,
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
