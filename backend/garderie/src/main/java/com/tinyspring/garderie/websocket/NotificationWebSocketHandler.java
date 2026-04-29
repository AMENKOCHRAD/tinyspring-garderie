package com.tinyspring.garderie.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinyspring.garderie.dto.RealtimeNotificationDto;
import com.tinyspring.garderie.security.CustomUserDetailsService;
import com.tinyspring.garderie.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    // email -> sessions
    private final Map<String, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();
    // role -> sessions (authority like ROLE_PARENT / ROLE_ANIMATRICE)
    private final Map<String, Set<WebSocketSession>> sessionsByRole = new ConcurrentHashMap<>();
    // sessionId -> email
    private final Map<String, String> userBySessionId = new ConcurrentHashMap<>();
    // sessionId -> roles
    private final Map<String, Set<String>> rolesBySessionId = new ConcurrentHashMap<>();

    public NotificationWebSocketHandler(JwtService jwtService,
                                        CustomUserDetailsService userDetailsService,
                                        ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractToken(session.getUri());
        if (token == null || token.isBlank()) {
            session.close(new CloseStatus(HttpStatus.UNAUTHORIZED.value(), "Token manquant"));
            return;
        }

        String email;
        try {
            email = jwtService.extractUsername(token);
        } catch (Exception ex) {
            session.close(new CloseStatus(HttpStatus.UNAUTHORIZED.value(), "Token invalide"));
            return;
        }

        Set<String> roles = ConcurrentHashMap.newKeySet();
        try {
            var userDetails = userDetailsService.loadUserByUsername(email);
            if (!jwtService.isTokenValid(token, userDetails)) {
                session.close(new CloseStatus(HttpStatus.UNAUTHORIZED.value(), "Token invalide"));
                return;
            }
            userDetails.getAuthorities().forEach(a -> {
                if (a != null && a.getAuthority() != null && !a.getAuthority().isBlank()) {
                    roles.add(a.getAuthority().trim());
                }
            });
        } catch (Exception ex) {
            session.close(new CloseStatus(HttpStatus.UNAUTHORIZED.value(), "Token invalide"));
            return;
        }

        String normalized = email.trim().toLowerCase();
        sessionsByUser.computeIfAbsent(normalized, k -> ConcurrentHashMap.newKeySet()).add(session);
        userBySessionId.put(session.getId(), normalized);

        if (!roles.isEmpty()) {
            rolesBySessionId.put(session.getId(), roles);
            for (String role : roles) {
                sessionsByRole.computeIfAbsent(role, k -> ConcurrentHashMap.newKeySet()).add(session);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String user = userBySessionId.remove(session.getId());
        Set<String> roles = rolesBySessionId.remove(session.getId());

        if (user != null) {
            Set<WebSocketSession> sessions = sessionsByUser.get(user);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    sessionsByUser.remove(user);
                }
            }
        }

        if (roles != null) {
            for (String role : roles) {
                Set<WebSocketSession> sessions = sessionsByRole.get(role);
                if (sessions == null) continue;
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    sessionsByRole.remove(role);
                }
            }
        }
    }

    public void sendToUser(String email, RealtimeNotificationDto payload) {
        if (email == null || email.isBlank() || payload == null) {
            return;
        }

        String normalized = email.trim().toLowerCase();
        Set<WebSocketSession> sessions = sessionsByUser.get(normalized);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return;
        }

        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (session == null || !session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(message);
            } catch (Exception ignore) {
                // ignore single session failure
            }
        }
    }

    public void sendToRole(String authorityRole, RealtimeNotificationDto payload) {
        if (authorityRole == null || authorityRole.isBlank() || payload == null) {
            return;
        }

        String role = authorityRole.trim();
        Set<WebSocketSession> sessions = sessionsByRole.get(role);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return;
        }

        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (session == null || !session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(message);
            } catch (Exception ignore) {
                // ignore single session failure
            }
        }
    }

    private static String extractToken(URI uri) {
        if (uri == null || uri.getRawQuery() == null || uri.getRawQuery().isBlank()) {
            return null;
        }

        String[] parts = uri.getRawQuery().split("&");
        for (String part : parts) {
            int idx = part.indexOf('=');
            if (idx <= 0) continue;
            String key = part.substring(0, idx);
            if (!"token".equalsIgnoreCase(key)) continue;
            String value = part.substring(idx + 1);
            try {
                return URLDecoder.decode(value, StandardCharsets.UTF_8);
            } catch (Exception ex) {
                return value;
            }
        }
        return null;
    }
}
