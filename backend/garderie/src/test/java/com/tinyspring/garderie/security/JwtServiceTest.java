package com.tinyspring.garderie.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        String secret = Base64.getEncoder().encodeToString(
                "test-secret-key-that-is-long-enough-123".getBytes(StandardCharsets.UTF_8)
        );
        ReflectionTestUtils.setField(jwtService, "secretKey", secret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 60_000L);
    }

    @Test
    void shouldGenerateTokenAndExtractUsername() {
        UserDetails userDetails = User.withUsername("parent@test.tn")
                .password("encoded")
                .authorities("ROLE_PARENT")
                .build();

        String token = jwtService.generateToken(userDetails, Map.of("role", "PARENT"));

        assertEquals("parent@test.tn", jwtService.extractUsername(token));
        assertEquals("PARENT", jwtService.extractClaim(token, claims -> claims.get("role", String.class)));
    }

    @Test
    void shouldValidateTokenForMatchingUser() {
        UserDetails userDetails = User.withUsername("admin@test.tn")
                .password("encoded")
                .authorities("ROLE_ADMIN")
                .build();

        String token = jwtService.generateToken(userDetails, Map.of());

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void shouldRejectTokenForDifferentUser() {
        UserDetails tokenOwner = User.withUsername("admin@test.tn")
                .password("encoded")
                .authorities("ROLE_ADMIN")
                .build();
        UserDetails anotherUser = User.withUsername("parent@test.tn")
                .password("encoded")
                .authorities("ROLE_PARENT")
                .build();

        String token = jwtService.generateToken(tokenOwner, Map.of());

        assertFalse(jwtService.isTokenValid(token, anotherUser));
    }

    @Test
    void shouldExposeConfiguredExpirationTime() {
        assertEquals(60_000L, jwtService.getExpirationTime());
    }
}
