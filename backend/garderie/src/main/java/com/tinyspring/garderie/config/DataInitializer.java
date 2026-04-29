package com.tinyspring.garderie.config;

import com.tinyspring.garderie.entity.Role;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.repository.RoleRepository;
import com.tinyspring.garderie.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(RoleRepository roleRepository,
                                   UserRepository userRepository,
                                   PasswordEncoder passwordEncoder) {
        return args -> {

            Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                    .orElseGet(() -> roleRepository.save(new Role(RoleName.ADMIN)));

            Role parentRole = roleRepository.findByName(RoleName.PARENT)
                    .orElseGet(() -> roleRepository.save(new Role(RoleName.PARENT)));

            Role animatriceRole = roleRepository.findByName(RoleName.ANIMATRICE)
                    .orElseGet(() -> roleRepository.save(new Role(RoleName.ANIMATRICE)));

            if (userRepository.findByEmailIgnoreCase("admin@garderie.com").isEmpty()) {
                userRepository.save(new User(
                        "Admin Principal",
                        "admin@garderie.com",
                        passwordEncoder.encode("admin123"),
                        true,
                        adminRole
                ));
            }

            if (userRepository.findByEmailIgnoreCase("parent@garderie.com").isEmpty()) {
                userRepository.save(new User(
                        "Parent Test",
                        "parent@garderie.com",
                        passwordEncoder.encode("parent123"),
                        true,
                        parentRole
                ));
            }

            if (userRepository.findByEmailIgnoreCase("animatrice@garderie.com").isEmpty()) {
                userRepository.save(new User(
                        "Animatrice Test",
                        "animatrice@garderie.com",
                        passwordEncoder.encode("anim123"),
                        true,
                        animatriceRole
                ));
            }
        };
    }
}
