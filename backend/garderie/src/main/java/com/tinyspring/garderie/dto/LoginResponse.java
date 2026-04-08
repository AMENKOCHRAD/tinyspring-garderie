package com.tinyspring.garderie.dto;

public class LoginResponse {

    private String message;
    private Long userId;
    private String nom;
    private String email;
    private String role;
    private String token;

    public LoginResponse() {
    }

    public LoginResponse(String message, Long userId, String nom, String email, String role, String token) {
        this.message = message;
        this.userId = userId;
        this.nom = nom;
        this.email = email;
        this.role = role;
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public Long getUserId() {
        return userId;
    }

    public String getNom() {
        return nom;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }


    public String getToken() {
        return token;
    }



    public void setMessage(String message) {
        this.message = message;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRole(String role) {
        this.role = role;
    }
    public void setToken(String token) {
        this.token = token;
    }
}