package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.AuthResponse;
import com.tinyspring.garderie.dto.LoginRequest;
import com.tinyspring.garderie.entity.Role;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
    private JwtService jwtService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(userRepository, authenticationManager, jwtService);
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
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("admin@garderie.com")).thenReturn(Optional.of(admin));
        when(jwtService.generateToken(org.mockito.ArgumentMatchers.eq(userDetails), org.mockito.ArgumentMatchers.anyMap()))
                .thenReturn("jwt-token");
        when(jwtService.getExpirationTime()).thenReturn(86400000L);

        ResponseEntity<?> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AuthResponse body = assertInstanceOf(AuthResponse.class, response.getBody());
        assertEquals("admin@garderie.com", body.email());
        assertEquals("ADMIN", body.role());
        assertEquals("jwt-token", body.accessToken());
    }

    @Test
    void shouldReturnUnauthorizedForBadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@garderie.com");
        request.setPassword("wrong");

        when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        ResponseEntity<?> response = authController.login(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Email ou mot de passe incorrect", response.getBody());
    }

    @Test
    void shouldReturnForbiddenForDisabledAccount() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@garderie.com");
        request.setPassword("admin123");

        when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("disabled"));

        ResponseEntity<?> response = authController.login(request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Votre compte est desactive", response.getBody());
    }

    @Test
    void shouldReturnNotFoundWhenAuthenticatedUserNoLongerExists() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ghost@garderie.com");
        request.setPassword("secret");

        UserDetails userDetails = org.springframework.security.core.userdetails.User.withUsername("ghost@garderie.com")
                .password("encoded")
                .authorities("ROLE_ADMIN")
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("ghost@garderie.com")).thenReturn(Optional.empty());

        ResponseEntity<?> response = authController.login(request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Utilisateur introuvable", response.getBody());
    }

    @Test
    void shouldReturnAuthenticatedUserProfile() {
        com.tinyspring.garderie.entity.User admin = buildUser("Admin Principal", "admin@garderie.com", RoleName.ADMIN);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn("admin@garderie.com");
        when(userRepository.findByEmail("admin@garderie.com")).thenReturn(Optional.of(admin));

        ResponseEntity<?> response = authController.me(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("admin@garderie.com"));
    }

    @Test
    void shouldReturnNotFoundForMissingAuthenticatedUserProfile() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("ghost@garderie.com");
        when(userRepository.findByEmail("ghost@garderie.com")).thenReturn(Optional.empty());

        ResponseEntity<?> response = authController.me(authentication);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Utilisateur introuvable", response.getBody());
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
