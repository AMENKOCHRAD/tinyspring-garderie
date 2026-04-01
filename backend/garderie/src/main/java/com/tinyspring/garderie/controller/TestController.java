package com.tinyspring.garderie.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/auth/test")
    public String publicTest() {
        return "API publique OK";
    }

    @GetMapping("/api/admin/test")
    public String adminTest() {
        return "Bienvenue Admin";
    }

    @GetMapping("/api/parent/test")
    public String parentTest() {
        return "Bienvenue Parent";
    }

    @GetMapping("/api/animatrice/test")
    public String animatriceTest() {
        return "Bienvenue Animatrice";
    }
}