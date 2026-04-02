package com.tinyspring.garderie.config;

import com.tinyspring.garderie.entity.Role;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.transport.Enfant;
import com.tinyspring.garderie.entity.transport.Trajet;
import com.tinyspring.garderie.entity.transport.Transport;
import com.tinyspring.garderie.repository.RoleRepository;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.repository.transport.EnfantRepository;
import com.tinyspring.garderie.repository.transport.TrajetRepository;
import com.tinyspring.garderie.repository.transport.TransportRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(RoleRepository roleRepository,
                                   UserRepository userRepository,
                                   EnfantRepository enfantRepository,
                                   TransportRepository transportRepository,
                                   TrajetRepository trajetRepository,
                                   PasswordEncoder passwordEncoder) {
        return args -> {

            Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                    .orElseGet(() -> roleRepository.save(new Role(RoleName.ADMIN)));

            Role parentRole = roleRepository.findByName(RoleName.PARENT)
                    .orElseGet(() -> roleRepository.save(new Role(RoleName.PARENT)));

            Role animatriceRole = roleRepository.findByName(RoleName.ANIMATRICE)
                    .orElseGet(() -> roleRepository.save(new Role(RoleName.ANIMATRICE)));

            if (userRepository.findByEmail("admin@garderie.com").isEmpty()) {
                userRepository.save(new User(
                        "Admin Principal",
                        "admin@garderie.com",
                        passwordEncoder.encode("admin123"),
                        true,
                        adminRole
                ));
            }

            if (userRepository.findByEmail("parent@garderie.com").isEmpty()) {
                userRepository.save(new User(
                        "Parent Test",
                        "parent@garderie.com",
                        passwordEncoder.encode("parent123"),
                        true,
                        parentRole
                ));
            }

            if (userRepository.findByEmail("animatrice@garderie.com").isEmpty()) {
                userRepository.save(new User(
                        "Animatrice Test",
                        "animatrice@garderie.com",
                        passwordEncoder.encode("anim123"),
                        true,
                        animatriceRole
                ));
            }

            User parent = userRepository.findByEmail("parent@garderie.com").orElseThrow();

            if (enfantRepository.findByParentId(parent.getId()).isEmpty()) {
                enfantRepository.save(new Enfant("Ben Salah", "Yasmine", parent));
                enfantRepository.save(new Enfant("Ben Salah", "Adam", parent));
            }

            Transport bus1;
            Transport bus2;

            if (transportRepository.count() == 0) {
                bus1 = transportRepository.save(new Transport("Mini Bus A", "TN-101", 12));
                bus2 = transportRepository.save(new Transport("Mini Bus B", "TN-102", 10));

                trajetRepository.save(new Trajet("Centre Ville", "Garderie Les Petits", LocalDate.now().plusDays(1), LocalTime.of(7, 30), bus1));
                trajetRepository.save(new Trajet("Lac 1", "Garderie Les Petits", LocalDate.now().plusDays(2), LocalTime.of(8, 0), bus2));
            } else {
                List<Transport> transports = transportRepository.findAll();
                bus1 = transports.get(0);
                bus2 = transports.size() > 1 ? transports.get(1) : transports.get(0);
            }

            List<Trajet> trajets = trajetRepository.findAll();
            if (trajets.isEmpty()) {
                trajetRepository.save(new Trajet("Centre Ville", "Garderie Les Petits", LocalDate.now().plusDays(1), LocalTime.of(7, 30), bus1));
                trajetRepository.save(new Trajet("Lac 1", "Garderie Les Petits", LocalDate.now().plusDays(2), LocalTime.of(8, 0), bus2));
            } else {
                for (Trajet trajet : trajets) {
                    if (trajet.getTransport() == null) {
                        trajet.setTransport(bus1);
                    }
                    if (trajet.getDateTrajet() == null || !trajet.getDateTrajet().isAfter(LocalDate.now())) {
                        trajet.setDateTrajet(LocalDate.now().plusDays(1));
                    }
                    trajetRepository.save(trajet);
                }
            }
        };
    }
}
