package com.tinyspring.garderie.config;

import com.tinyspring.garderie.entity.Role;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.transport.DemandeTransport;
import com.tinyspring.garderie.entity.transport.Enfant;
import com.tinyspring.garderie.entity.transport.SensTrajetDemandeTransport;
import com.tinyspring.garderie.entity.transport.StatutDemandeTransport;
import com.tinyspring.garderie.entity.transport.Trajet;
import com.tinyspring.garderie.entity.transport.Transport;
import com.tinyspring.garderie.repository.RoleRepository;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.repository.transport.DemandeTransportRepository;
import com.tinyspring.garderie.repository.transport.EnfantRepository;
import com.tinyspring.garderie.repository.transport.TrajetRepository;
import com.tinyspring.garderie.repository.transport.TransportRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@Profile("!test")
public class DataInitializer {

    private static final String ADRESSE_GARDERIE_EXACTE = "15 Rue des Ecoles, El Menzah 5, Ariana 2091, Tunisie";

    @Bean
    CommandLineRunner initDatabase(RoleRepository roleRepository,
                                   UserRepository userRepository,
                                   EnfantRepository enfantRepository,
                                   DemandeTransportRepository demandeTransportRepository,
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

            ensureUser(userRepository, passwordEncoder, adminRole, "Admin Principal", "admin@garderie.com", "admin123");
            ensureUser(userRepository, passwordEncoder, parentRole, "Parent Test", "parent@garderie.com", "parent123");
            ensureUser(userRepository, passwordEncoder, animatriceRole, "Animatrice Test", "animatrice@garderie.com", "anim123");
            ensureUser(userRepository, passwordEncoder, parentRole, "Sonia Trabelsi", "parent2@garderie.com", "parent123");
            ensureUser(userRepository, passwordEncoder, parentRole, "Yassine Gharbi", "parent3@garderie.com", "parent123");
            ensureUser(userRepository, passwordEncoder, parentRole, "Amira Ben Amor", "parent4@garderie.com", "parent123");

            User parent = userRepository.findByEmail("parent@garderie.com").orElseThrow();
            User parent2 = userRepository.findByEmail("parent2@garderie.com").orElseThrow();
            User parent3 = userRepository.findByEmail("parent3@garderie.com").orElseThrow();
            User parent4 = userRepository.findByEmail("parent4@garderie.com").orElseThrow();

            ensureChildren(enfantRepository, parent, List.of("Yasmine", "Adam"), "Ben Salah");
            ensureChildren(enfantRepository, parent2, List.of("Lina", "Aya"), "Trabelsi");
            ensureChildren(enfantRepository, parent3, List.of("Sami", "Rayen"), "Gharbi");
            ensureChildren(enfantRepository, parent4, List.of("Nour", "Malek"), "Ben Amor");

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

            ensureDemoTrajets(trajetRepository, bus1, bus2);
            seedTransportLoadTests(userRepository, enfantRepository, demandeTransportRepository, parentRole, passwordEncoder);
        };
    }

    private void ensureUser(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            Role role,
                            String nom,
                            String email,
                            String password) {
        if (userRepository.findByEmail(email).isEmpty()) {
            userRepository.save(new User(
                    nom,
                    email,
                    passwordEncoder.encode(password),
                    true,
                    role
            ));
        }
    }

    private void ensureChildren(EnfantRepository enfantRepository, User parent, List<String> prenoms, String nomFamille) {
        if (!enfantRepository.findByParentId(parent.getId()).isEmpty()) {
            return;
        }

        for (String prenom : prenoms) {
            enfantRepository.save(new Enfant(nomFamille, prenom, parent));
        }
    }

    private void ensureDemoTrajets(TrajetRepository trajetRepository, Transport bus1, Transport bus2) {
        List<Trajet> trajets = trajetRepository.findAll();
        if (trajets.isEmpty()) {
            creerTrajetsDemo(trajetRepository, bus1, bus2);
            return;
        }

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

    private void creerTrajetsDemo(TrajetRepository trajetRepository, Transport bus1, Transport bus2) {
        trajetRepository.save(new Trajet("Lac 1, Tunis", ADRESSE_GARDERIE_EXACTE, LocalDate.now().plusDays(1), LocalTime.of(7, 30), bus1));
        trajetRepository.save(new Trajet("Centre Ville, Tunis", ADRESSE_GARDERIE_EXACTE, LocalDate.now().plusDays(1), LocalTime.of(7, 50), bus2));
        trajetRepository.save(new Trajet(ADRESSE_GARDERIE_EXACTE, "Lac 1, Tunis", LocalDate.now().plusDays(1), LocalTime.of(16, 30), bus1));
        trajetRepository.save(new Trajet(ADRESSE_GARDERIE_EXACTE, "Centre Ville, Tunis", LocalDate.now().plusDays(1), LocalTime.of(17, 0), bus2));
    }

    private void seedTransportLoadTests(UserRepository userRepository,
                                        EnfantRepository enfantRepository,
                                        DemandeTransportRepository demandeTransportRepository,
                                        Role parentRole,
                                        PasswordEncoder passwordEncoder) {
        List<SeedParentDefinition> parents = List.of(
                new SeedParentDefinition("Cluster Borj 1", "parent5@garderie.com", List.of("Nassim", "Ines")),
                new SeedParentDefinition("Cluster Borj 2", "parent6@garderie.com", List.of("Youssef", "Meriem")),
                new SeedParentDefinition("Cluster Borj 3", "parent7@garderie.com", List.of("Salma", "Iyed")),
                new SeedParentDefinition("Cluster Kalaat 1", "parent8@garderie.com", List.of("Rym", "Kais")),
                new SeedParentDefinition("Cluster Kalaat 2", "parent9@garderie.com", List.of("Maya", "Skander")),
                new SeedParentDefinition("Cluster Kalaat 3", "parent10@garderie.com", List.of("Nourhene", "Aziz"))
        );

        Map<String, List<Enfant>> enfantsParParent = new LinkedHashMap<>();
        for (SeedParentDefinition parentDefinition : parents) {
            User parent = userRepository.findByEmail(parentDefinition.email())
                    .orElseGet(() -> userRepository.save(new User(
                            parentDefinition.nom(),
                            parentDefinition.email(),
                            passwordEncoder.encode("parent123"),
                            true,
                            parentRole
                    )));

            List<Enfant> enfants = enfantRepository.findByParentId(parent.getId());
            if (enfants.isEmpty()) {
                List<Enfant> nouveauxEnfants = new ArrayList<>();
                for (String prenom : parentDefinition.enfants()) {
                    nouveauxEnfants.add(enfantRepository.save(new Enfant(parentDefinition.nom(), prenom, parent)));
                }
                enfants = nouveauxEnfants;
            }

            enfantsParParent.put(parentDefinition.email(), enfants);
        }

        List<SeedDemandeDefinition> demandes = List.of(
                new SeedDemandeDefinition("parent5@garderie.com", 0, "Borj Touil, Raoued, Ariana, Tunisie", 36.9270, 10.1920, LocalTime.of(7, 35)),
                new SeedDemandeDefinition("parent6@garderie.com", 0, "Borj Touil, Raoued, Ariana, Tunisie", 36.9284, 10.1931, LocalTime.of(7, 40)),
                new SeedDemandeDefinition("parent7@garderie.com", 0, "Borj Touil, Raoued, Ariana, Tunisie", 36.9262, 10.1915, LocalTime.of(7, 45)),
                new SeedDemandeDefinition("parent5@garderie.com", 1, "Borj Touil, Raoued, Ariana, Tunisie", 36.9291, 10.1944, LocalTime.of(7, 50)),
                new SeedDemandeDefinition("parent8@garderie.com", 0, "Kalaat El Andalous, Ariana, Tunisie", 37.0625, 10.1180, LocalTime.of(7, 30)),
                new SeedDemandeDefinition("parent9@garderie.com", 0, "Kalaat El Andalous, Ariana, Tunisie", 37.0610, 10.1204, LocalTime.of(7, 35)),
                new SeedDemandeDefinition("parent10@garderie.com", 0, "Kalaat El Andalous, Ariana, Tunisie", 37.0636, 10.1168, LocalTime.of(7, 40)),
                new SeedDemandeDefinition("parent8@garderie.com", 1, "Kalaat El Andalous, Ariana, Tunisie", 37.0641, 10.1210, LocalTime.of(7, 45)),
                new SeedDemandeDefinition("parent9@garderie.com", 1, "Kalaat El Andalous, Ariana, Tunisie", 37.0604, 10.1179, LocalTime.of(7, 50)),
                new SeedDemandeDefinition("parent10@garderie.com", 1, "Kalaat El Andalous, Ariana, Tunisie", 37.0620, 10.1192, LocalTime.of(7, 55))
        );

        LocalDate dateSouhaitee = LocalDate.now().plusDays(1);
        for (SeedDemandeDefinition demandeDefinition : demandes) {
            List<Enfant> enfants = enfantsParParent.get(demandeDefinition.parentEmail());
            if (enfants == null || enfants.size() <= demandeDefinition.childIndex()) {
                continue;
            }

            Enfant enfant = enfants.get(demandeDefinition.childIndex());
            if (!demandeTransportRepository.findTop10ByEnfantIdOrderByIdDesc(enfant.getId()).isEmpty()) {
                continue;
            }

            DemandeTransport demande = new DemandeTransport(
                    enfant,
                    enfant.getParent(),
                    null,
                    StatutDemandeTransport.EN_ATTENTE,
                    demandeDefinition.adresseMaison(),
                    ADRESSE_GARDERIE_EXACTE,
                    SensTrajetDemandeTransport.MAISON_VERS_GARDERIE,
                    demandeDefinition.adresseMaison(),
                    demandeDefinition.latitudeMaison(),
                    demandeDefinition.longitudeMaison(),
                    dateSouhaitee,
                    demandeDefinition.heureSouhaitee()
            );
            demande.setAiAnalysisAvailable(true);
            demande.setSuspicious(false);
            demande.setDuplicateDetected(false);
            demande.setAnomalyScore(0.0);
            demande.setAnomalyLevel("LOW");
            demande.setAnomalyReasons("Jeu de donnees de demonstration pour les recommandations");
            demande.setAiModelVersion("seed-demo");
            demande.setAiAnalysisError(null);

            demandeTransportRepository.save(demande);
        }
    }

    private record SeedParentDefinition(String nom, String email, List<String> enfants) {
    }

    private record SeedDemandeDefinition(String parentEmail,
                                         int childIndex,
                                         String adresseMaison,
                                         double latitudeMaison,
                                         double longitudeMaison,
                                         LocalTime heureSouhaitee) {
    }
}
