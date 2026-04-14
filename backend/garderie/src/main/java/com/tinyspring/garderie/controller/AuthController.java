package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.LoginRequest;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.security.CustomUserDetailsService;
import com.tinyspring.garderie.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          CustomUserDetailsService customUserDetailsService,
                          JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.customUserDetailsService = customUserDetailsService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        // 1. Vérifier l'email
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Utilisateur introuvable");
        }

        // 2. Vérifier le mot de passe
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Mot de passe incorrect");
        }

        // 3. Charger UserDetails via CustomUserDetailsService
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(user.getEmail());

        // 4. Générer le token JWT avec rôle + userId
        String accessToken = jwtService.generateToken(userDetails, Map.of(
                "role", user.getRole().getName().name(),
                "userId", user.getId()
        ));

        // 5. Retourner la réponse complète
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("nom", user.getNom());
        response.put("message", "Login success");
        response.put("email", user.getEmail());
        response.put("role", user.getRole().getName().name());
        response.put("accessToken", accessToken);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", jwtService.getExpirationTime());

        return ResponseEntity.ok(response);
    }
}