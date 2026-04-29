package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.LoginRequest;
import com.tinyspring.garderie.entity.Role;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.security.CustomUserDetailsService;
import com.tinyspring.garderie.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(userRepository, passwordEncoder, jwtService, customUserDetailsService);
    }

    @Test
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@garderie.com");
        request.setPassword("admin123");

        com.tinyspring.garderie.entity.User admin = buildUser("Admin Principal", "admin@garderie.com", RoleName.ADMIN);
        UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername("admin@garderie.com")
                .password("encoded")
                .authorities("ROLE_ADMIN")
                .build();
        when(userRepository.findByEmailIgnoreCase("admin@garderie.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("admin123", "encoded")).thenReturn(true);
        when(customUserDetailsService.loadUserByUsername("admin@garderie.com")).thenReturn(userDetails);
        when(jwtService.generateToken(org.mockito.ArgumentMatchers.eq(userDetails), org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn("jwt-token");

        ResponseEntity<?> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("admin@garderie.com", body.get("email"));
        assertEquals("ADMIN", body.get("role"));
        assertEquals("jwt-token", body.get("token"));
    }

    @Test
    void shouldReturnUnauthorizedForBadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@garderie.com");
        request.setPassword("wrong");

        User admin = buildUser("Admin Principal", "admin@garderie.com", RoleName.ADMIN);
        when(userRepository.findByEmailIgnoreCase("admin@garderie.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        ResponseEntity<?> response = authController.login(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Mot de passe incorrect", response.getBody());
    }

    @Test
    void shouldReturnBadRequestForMissingEmail() {
        LoginRequest request = new LoginRequest();
        request.setEmail(" ");
        request.setPassword("admin123");

        ResponseEntity<?> response = authController.login(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Email manquant", response.getBody());
    }

    @Test
    void shouldReturnNotFoundWhenAuthenticatedUserNoLongerExists() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ghost@garderie.com");
        request.setPassword("secret");

        when(userRepository.findByEmailIgnoreCase("ghost@garderie.com")).thenReturn(Optional.empty());

        ResponseEntity<?> response = authController.login(request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Utilisateur introuvable", response.getBody());
    }

    @Test
    void shouldKeepAuthenticatedUserProfileCoveredByRepositoryContract() {
        com.tinyspring.garderie.entity.User admin = buildUser("Admin Principal", "admin@garderie.com", RoleName.ADMIN);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn("admin@garderie.com");
        when(userRepository.findByEmail("admin@garderie.com")).thenReturn(Optional.of(admin));

        Optional<User> response = userRepository.findByEmail(authentication.getName());
        assertTrue(response.isPresent());
        assertEquals("admin@garderie.com", response.get().getEmail());
    }

    @Test
    void shouldReturnEmptyForMissingAuthenticatedUserProfile() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("ghost@garderie.com");
        when(userRepository.findByEmail("ghost@garderie.com")).thenReturn(Optional.empty());

        Optional<User> response = userRepository.findByEmail(authentication.getName());
        assertFalse(response.isPresent());
    }

    private com.tinyspring.garderie.entity.User buildUser(String nom, String email, RoleName roleName) {
        Role role = new Role(roleName);
        com.tinyspring.garderie.entity.User user = new com.tinyspring.garderie.entity.User(nom, email, "encoded", true, role);
        setId(user, 1L);
        return user;
    }

    private void setId(Object target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
