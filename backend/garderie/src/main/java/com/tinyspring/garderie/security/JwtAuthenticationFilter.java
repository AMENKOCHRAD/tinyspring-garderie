package com.tinyspring.garderie.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        // ✅ 1. Lire le token depuis le header Authorization (requêtes HTTP normales)
        String jwt = null;
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
            System.out.println("🔍 Token lu depuis le header Authorization");
        }

        // ✅ 2. Fallback : lire depuis le query param ?token= (SSE EventSource)
        //    EventSource ne supporte pas les headers custom,
        //    donc le frontend passe le token en query param pour le stream SSE
        if (jwt == null) {
            String tokenParam = request.getParameter("token");
            if (tokenParam != null && !tokenParam.isBlank()) {
                jwt = tokenParam;
                System.out.println("🔍 Token lu depuis le query param ?token= (SSE)");
            }
        }

        System.out.println("🔍 URL: " + request.getRequestURI());

        // Pas de token du tout → on passe au filtre suivant (Spring Security gérera l'accès)
        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ 3. Extraire l'email depuis le token
        String userEmail;
        try {
            userEmail = jwtService.extractUsername(jwt);
            System.out.println("✅ Email extrait: " + userEmail);
        } catch (Exception exception) {
            System.out.println("❌ Erreur extraction token: " + exception.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ 4. Authentifier si pas encore authentifié
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            System.out.println("✅ Authorities: " + userDetails.getAuthorities());
            System.out.println("✅ Token valide: " + jwtService.isTokenValid(jwt, userDetails));

            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                System.out.println("✅ Authentification réussie pour: " + userEmail);
            } else {
                System.out.println("❌ Token invalide pour: " + userEmail);
            }
        }

        filterChain.doFilter(request, response);
    }
}