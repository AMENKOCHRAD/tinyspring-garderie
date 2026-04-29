package com.tinyspring.garderie.config;

import com.tinyspring.garderie.security.CustomUserDetailsService;
import com.tinyspring.garderie.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity
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
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider(userDetailsService);
        auth.setPasswordEncoder(passwordEncoder);
        return auth;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
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
<<<<<<< HEAD
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
=======
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/test").permitAll()
                        .requestMatchers("/api/classes/**").permitAll()
                        // public events
                        .requestMatchers(HttpMethod.GET, "/api/events/public").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/public/*").permitAll()
                        // =========================
                        // EVENTS - LECTURE
                        // =========================
                        .requestMatchers(HttpMethod.GET, "/api/events/get").hasAnyRole("ADMIN", "PARENT")
                        .requestMatchers(HttpMethod.GET, "/api/events/*").hasAnyRole("ADMIN", "PARENT")
                        // affichage détails des avis admin
                        .requestMatchers(HttpMethod.GET, "/api/events/*/ratings").hasRole("ADMIN")
                        // =========================
                        // EVENTS - ACTIONS ADMIN
                        // =========================
                        .requestMatchers(HttpMethod.POST, "/api/events/create").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/events/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/events/delete/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/events/*/publish").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/events/*/photo").hasRole("ADMIN")

                        // recommandation IA events -> ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/events/ai/recommend").hasRole("ADMIN")

                        // =========================
                        // REGISTRATIONS
                        // =========================
                        // création registration -> PARENT
                        .requestMatchers(HttpMethod.POST, "/api/events/*/registrations").hasRole("PARENT")

                        // affichage registrations par event -> ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/events/*/registrations").hasRole("ADMIN")

                        // confirm / cancel -> PARENT
                        .requestMatchers(HttpMethod.PUT, "/api/registrations/*/confirm").hasRole("PARENT")
                        .requestMatchers(HttpMethod.PUT, "/api/registrations/*/cancel").hasRole("PARENT")

                        // attended / absent -> ADMIN
                        .requestMatchers(HttpMethod.PUT, "/api/registrations/*/attended").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/registrations/*/absent").hasRole("ADMIN")

// =========================
                                // RATINGS PARENT
                                // =========================
                                .requestMatchers(HttpMethod.POST, "/api/parent/events/*/rating").hasRole("PARENT")

                                // =========================
                                // MENUS WEEKLY
                                // =========================
                                // affichage -> ADMIN + PARENT
                                .requestMatchers(HttpMethod.GET, "/api/menus/weekly").hasAnyRole("ADMIN", "PARENT")
                                .requestMatchers(HttpMethod.GET, "/api/menus/weekly/*").hasAnyRole("ADMIN", "PARENT")

                                // create / update / delete / duplicate -> ADMIN
                                .requestMatchers(HttpMethod.POST, "/api/menus/weekly").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.PUT, "/api/menus/weekly/*").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/api/menus/weekly/*").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.POST, "/api/menus/weekly/*/duplicate").hasRole("ADMIN")

                                // IA weekly menu -> ADMIN
                                .requestMatchers(HttpMethod.GET, "/api/menus/weekly/ai/ping").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.POST, "/api/menus/weekly/ai/post-test").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.POST, "/api/menus/weekly/ai/echo").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.POST, "/api/menus/weekly/ai/generate-test").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.POST, "/api/menus/weekly/ai/generate").hasRole("ADMIN")

                                // =========================
                                // MENUS DAILY
                                // =========================
                                // create / update / delete -> ADMIN
                                .requestMatchers(HttpMethod.POST, "/api/menus/daily").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.PUT, "/api/menus/daily/*").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/api/menus/daily/*").hasRole("ADMIN")
                                .requestMatchers("/api/parent/notifications/**").permitAll()


                                .requestMatchers(HttpMethod.GET, "/api/menus/daily/**").hasAnyRole("ADMIN", "PARENT")

                                // =========================
                                // MENUS DISHES
                                // =========================
                                // create / update / delete -> ADMIN
                                .requestMatchers(HttpMethod.POST, "/api/menus/dishes").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.PUT, "/api/menus/dishes/*").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/api/menus/dishes/*").hasRole("ADMIN")


                                .requestMatchers(HttpMethod.GET, "/api/menus/dishes/**").hasAnyRole("ADMIN", "PARENT")


                        // visible to admin and parent
                        .requestMatchers(HttpMethod.GET, "/api/events/get").hasAnyRole("ADMIN", "PARENT")
                        .requestMatchers(HttpMethod.GET, "/api/events/*").hasAnyRole("ADMIN", "PARENT")




                        .requestMatchers(HttpMethod.POST, "/api/menus/weekly/ai/post-test").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/menus/weekly/ai/ping").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/menus/weekly/ai/generate").permitAll()
                        .requestMatchers("/api/menus/weekly/**").permitAll()
                        .requestMatchers("/api/menus/daily/**").permitAll()
                        .requestMatchers("/api/menus/dishes/**").permitAll()

>>>>>>> origin/gestion-evenements
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
                        .requestMatchers("/api/traitements/refuser/**").hasRole("ADMIN")
                        .requestMatchers("/api/traitements/*/validation-history").hasRole("ADMIN")
                        .requestMatchers("/api/traitements/validation-events").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/traitements/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/traitements/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/traitements/condition/**").hasAnyRole("ADMIN", "PARENT")
                        .requestMatchers(HttpMethod.GET, "/api/traitements/**").hasAnyRole("ADMIN", "PARENT", "ANIMATRICE")
                        .anyRequest().authenticated()
                )
<<<<<<< HEAD
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(AbstractHttpConfigurer::disable);
=======
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
>>>>>>> origin/gestion-evenements

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
<<<<<<< HEAD
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));
=======
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*"));
>>>>>>> origin/gestion-evenements
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
<<<<<<< HEAD
=======

>>>>>>> origin/gestion-evenements
}
