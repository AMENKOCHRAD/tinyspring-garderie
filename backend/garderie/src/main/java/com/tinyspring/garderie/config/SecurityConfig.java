package com.tinyspring.garderie.config;

import com.tinyspring.garderie.security.CustomUserDetailsService;
import com.tinyspring.garderie.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

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
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider(userDetailsService);
        auth.setPasswordEncoder(passwordEncoder);
        return auth;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"message\":\"Non authentifie (token manquant ou invalide).\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"message\":\"Acces refuse (role insuffisant).\"}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/parent/traitements/**").hasAnyRole("PARENT", "ADMIN")
                        .requestMatchers("/api/parent/**").hasAnyRole("PARENT", "ADMIN")
                        .requestMatchers("/api/animatrice/**").hasRole("ANIMATRICE")
                        .requestMatchers("/api/enfants/**").hasAnyRole("ADMIN", "ANIMATRICE", "PARENT")
                        .requestMatchers(HttpMethod.GET, "/api/conditions/**").hasAnyRole("ADMIN", "PARENT", "ANIMATRICE")
                        .requestMatchers(HttpMethod.POST, "/api/conditions/**").hasAnyRole("ADMIN", "PARENT")
                        .requestMatchers(HttpMethod.PUT, "/api/conditions/**").hasAnyRole("ADMIN", "PARENT")
                        .requestMatchers(HttpMethod.DELETE, "/api/conditions/**").hasAnyRole("ADMIN", "PARENT")
                        .requestMatchers("/api/traitements/en-attente-validation").hasRole("ADMIN")
                        .requestMatchers("/api/traitements/details/**").hasRole("ADMIN")
                        .requestMatchers("/api/traitements/valider/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/traitements/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/traitements/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/traitements/condition/**").hasAnyRole("ADMIN", "PARENT")
                        .requestMatchers(HttpMethod.GET, "/api/traitements/**").hasAnyRole("ADMIN", "PARENT", "ANIMATRICE")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"
        ));
        configuration.setExposedHeaders(List.of("Content-Disposition"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
