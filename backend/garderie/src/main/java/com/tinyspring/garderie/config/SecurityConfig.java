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
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
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

                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/parent/**").hasRole("PARENT")
                        .requestMatchers("/api/animatrice/**").hasRole("ANIMATRICE")
                        .requestMatchers("/api/enfants/**").hasAnyRole("ADMIN", "ANIMATRICE")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
