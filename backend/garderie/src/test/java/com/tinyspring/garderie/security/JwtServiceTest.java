package com.tinyspring.garderie.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests JwtService")
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(
                jwtService,
                "jwtSecret",
                "0123456789ABCDEF0123456789ABCDEF"
        );
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3_600_000L);
        jwtService.init();
    }

    @Test
    @DisplayName("generateToken() retourne un token signe")
    void generateToken_shouldReturnSignedToken() {
        UserDetails userDetails = userDetails("parent@example.com");

        String token = jwtService.generateToken(userDetails, Map.of("role", "PARENT"));

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("extractEmail() retourne le sujet du token")
    void extractEmail_shouldReturnTokenSubject() {
        UserDetails userDetails = userDetails("parent@example.com");
        String token = jwtService.generateToken(userDetails, Map.of());

        String email = jwtService.extractEmail(token);

        assertThat(email).isEqualTo("parent@example.com");
    }

    @Test
    @DisplayName("isTokenValid() retourne true pour le bon utilisateur")
    void isTokenValid_shouldReturnTrueForMatchingUser() {
        UserDetails userDetails = userDetails("parent@example.com");
        String token = jwtService.generateToken(userDetails, Map.of("role", "PARENT"));

        boolean valid = jwtService.isTokenValid(token, userDetails);

        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("isTokenValid() retourne false pour un autre utilisateur")
    void isTokenValid_shouldReturnFalseForDifferentUser() {
        UserDetails tokenOwner = userDetails("parent@example.com");
        UserDetails otherUser = userDetails("admin@example.com");
        String token = jwtService.generateToken(tokenOwner, Map.of());

        boolean valid = jwtService.isTokenValid(token, otherUser);

        assertThat(valid).isFalse();
    }

    private UserDetails userDetails(String email) {
        return User.withUsername(email)
                .password("secret")
                .authorities("ROLE_PARENT")
                .build();
    }
}
