package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/by-role/{roleName}")
    public List<User> getUsersByRole(@PathVariable String roleName) {
        try {
            RoleName role = RoleName.valueOf(roleName.toUpperCase());
            return userRepository.findByRole_Name(role);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Rôle invalide");
        }
    }
}