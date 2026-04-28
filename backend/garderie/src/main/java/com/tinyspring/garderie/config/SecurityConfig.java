package com.tinyspring.garderie.config;

import com.tinyspring.garderie.security.CustomUserDetailsService;
import com.tinyspring.garderie.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          PasswordEncoder passwordEncoder,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userDetailsService);
        auth.setPasswordEncoder(passwordEncoder);
        return auth;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth

                        // ── Auth publique ──────────────────────────────────
                        .requestMatchers("/api/auth/**").permitAll()

                        // ── Webhook Stripe ─────────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/stripe/webhook").permitAll()

                        // ✅ Magic links email — AVANT toute règle commandes/**
                        .requestMatchers(HttpMethod.GET,
                                "/api/boutique/commandes/action/especes/**",
                                "/api/boutique/commandes/action/refuser/**"
                        ).permitAll()

                        // ── Endpoint échec paiement — public ───────────────
                        .requestMatchers(HttpMethod.POST,
                                "/api/boutique/commandes/echec-par-session/**"
                        ).permitAll()

                        // ── Ressources publiques ───────────────────────────
                        .requestMatchers("/images/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/boutique/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/boutique/produits/**").permitAll()

                        // ── Boutique front-office authentifiée ─────────────
                        .requestMatchers(HttpMethod.POST, "/api/boutique/commandes").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/boutique/commandes/*/checkout-session").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/boutique/commandes/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/boutique/commandes/**").authenticated()

                        // ── Boutique back-office admin ─────────────────────
                        .requestMatchers("/api/admin/boutique/**").hasRole("ADMIN")

                        // ── Autres routes ──────────────────────────────────
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/parent/**").hasRole("PARENT")
                        .requestMatchers("/api/animatrice/**").hasRole("ANIMATRICE")
                        .requestMatchers("/api/enfants/**").hasAnyRole("ADMIN", "ANIMATRICE")
                        .requestMatchers(HttpMethod.GET, "/api/boutique/produits/recommandes").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/boutique/interactions").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/admin/boutique/affinites/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "http://localhost:21065"
        ));

        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        // ✅ Config CORS séparée pour le webhook Stripe
        CorsConfiguration stripeConfig = new CorsConfiguration();
        stripeConfig.setAllowedOrigins(List.of("*"));
        stripeConfig.setAllowedMethods(List.of("POST"));
        stripeConfig.setAllowedHeaders(List.of("*"));
        stripeConfig.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/stripe/webhook", stripeConfig);
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}