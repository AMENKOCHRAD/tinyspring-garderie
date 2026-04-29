package com.tinyspring.garderie.dto;

public class AuthResponse {

    private String message;
    private Long id;
    private String nom;
    private String email;
    private String role;
    private String accessToken;
    private String tokenType;
    private long expiresIn;

    public AuthResponse() {
    }

    public AuthResponse(String message,
                        Long id,
                        String nom,
                        String email,
                        String role,
                        String accessToken,
                        String tokenType,
                        long expiresIn) {
        this.message = message;
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.role = role;
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
