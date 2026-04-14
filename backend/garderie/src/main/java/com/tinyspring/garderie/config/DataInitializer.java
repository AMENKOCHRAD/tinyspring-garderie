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

    private static final String ADRESSE_GARDERIE_EXACTE = "15 Rue des Ecoles, El Menzah 5, Ariana 2091, Tunisie";

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

            if (userRepository.findByEmail("parent2@garderie.com").isEmpty()) {
                userRepository.save(new User(
                        "Sonia Trabelsi",
                        "parent2@garderie.com",
                        passwordEncoder.encode("parent123"),
                        true,
                        parentRole
                ));
            }

            if (userRepository.findByEmail("parent3@garderie.com").isEmpty()) {
                userRepository.save(new User(
                        "Yassine Gharbi",
                        "parent3@garderie.com",
                        passwordEncoder.encode("parent123"),
                        true,
                        parentRole
                ));
            }

            if (userRepository.findByEmail("parent4@garderie.com").isEmpty()) {
                userRepository.save(new User(
                        "Amira Ben Amor",
                        "parent4@garderie.com",
                        passwordEncoder.encode("parent123"),
                        true,
                        parentRole
                ));
            }

            User parent = userRepository.findByEmail("parent@garderie.com").orElseThrow();
            User parent2 = userRepository.findByEmail("parent2@garderie.com").orElseThrow();
            User parent3 = userRepository.findByEmail("parent3@garderie.com").orElseThrow();
            User parent4 = userRepository.findByEmail("parent4@garderie.com").orElseThrow();

            if (enfantRepository.findByParentId(parent.getId()).isEmpty()) {
                enfantRepository.save(new Enfant("Ben Salah", "Yasmine", parent));
                enfantRepository.save(new Enfant("Ben Salah", "Adam", parent));
            }

            if (enfantRepository.findByParentId(parent2.getId()).isEmpty()) {
                enfantRepository.save(new Enfant("Trabelsi", "Lina", parent2));
                enfantRepository.save(new Enfant("Trabelsi", "Aya", parent2));
            }

            if (enfantRepository.findByParentId(parent3.getId()).isEmpty()) {
                enfantRepository.save(new Enfant("Gharbi", "Sami", parent3));
                enfantRepository.save(new Enfant("Gharbi", "Rayen", parent3));
            }

            if (enfantRepository.findByParentId(parent4.getId()).isEmpty()) {
                enfantRepository.save(new Enfant("Ben Amor", "Nour", parent4));
                enfantRepository.save(new Enfant("Ben Amor", "Malek", parent4));
            }

            Transport bus1;
            Transport bus2;

            if (transportRepository.count() == 0) {
                bus1 = transportRepository.save(new Transport("Mini Bus A", "TN-101", 12));
                bus2 = transportRepository.save(new Transport("Mini Bus B", "TN-102", 10));

                creerTrajetsDemo(trajetRepository, bus1, bus2);
            } else {
                List<Transport> transports = transportRepository.findAll();
                bus1 = transports.get(0);
                bus2 = transports.size() > 1 ? transports.get(1) : transports.get(0);
            }

            List<Trajet> trajets = trajetRepository.findAll();
            if (trajets.isEmpty()) {
                creerTrajetsDemo(trajetRepository, bus1, bus2);
            } else {
                for (Trajet trajet : trajets) {
                    if (trajet.getTransport() == null) {
                        trajet.setTransport(bus1);
                    }
                    if ("Garderie Les Petits".equalsIgnoreCase(trajet.getDestination())) {
                        trajet.setDestination(ADRESSE_GARDERIE_EXACTE);
                    }
                    if ("Garderie Les Petits".equalsIgnoreCase(trajet.getPointDepart())) {
                        trajet.setPointDepart(ADRESSE_GARDERIE_EXACTE);
                    }
                    if (trajet.getDateTrajet() == null || !trajet.getDateTrajet().isAfter(LocalDate.now())) {
                        trajet.setDateTrajet(LocalDate.now().plusDays(1));
                    }
                    trajetRepository.save(trajet);
                }

                long trajetsDepuisGarderie = trajets.stream()
                        .filter(trajet -> ADRESSE_GARDERIE_EXACTE.equalsIgnoreCase(trajet.getPointDepart()))
                        .count();
                long trajetsVersGarderie = trajets.stream()
                        .filter(trajet -> ADRESSE_GARDERIE_EXACTE.equalsIgnoreCase(trajet.getDestination()))
                        .count();

                if (trajetsDepuisGarderie == 0 || trajetsVersGarderie == 0) {
                    creerTrajetsDemo(trajetRepository, bus1, bus2);
                }
            }
        };
    }

    private void creerTrajetsDemo(TrajetRepository trajetRepository, Transport bus1, Transport bus2) {
        trajetRepository.save(new Trajet("Lac 1, Tunis", ADRESSE_GARDERIE_EXACTE, LocalDate.now().plusDays(1), LocalTime.of(7, 30), bus1));
        trajetRepository.save(new Trajet("Centre Ville, Tunis", ADRESSE_GARDERIE_EXACTE, LocalDate.now().plusDays(1), LocalTime.of(7, 50), bus2));
        trajetRepository.save(new Trajet(ADRESSE_GARDERIE_EXACTE, "Lac 1, Tunis", LocalDate.now().plusDays(1), LocalTime.of(16, 30), bus1));
        trajetRepository.save(new Trajet(ADRESSE_GARDERIE_EXACTE, "Centre Ville, Tunis", LocalDate.now().plusDays(1), LocalTime.of(17, 0), bus2));
    }
}
