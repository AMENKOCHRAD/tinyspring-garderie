package com.tinyspring.garderie.repository.transport;

import com.tinyspring.garderie.entity.Role;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.repository.RoleRepository;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.entity.transport.DemandeTransport;
import com.tinyspring.garderie.entity.transport.Enfant;
import com.tinyspring.garderie.entity.transport.SensTrajetDemandeTransport;
import com.tinyspring.garderie.entity.transport.StatutDemandeTransport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DemandeTransportRepositoryTest {

    @Autowired
    private DemandeTransportRepository demandeTransportRepository;

    @Autowired
    private EnfantRepository enfantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Role parentRole;

    @BeforeEach
    void setUp() {
        parentRole = roleRepository.findByName(RoleName.PARENT)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.PARENT)));
    }

    @Test
    void shouldReturnPendingDemandesOrderedByOldestFirst() {
        DemandeTransport oldestPending = saveDemande(
                "Parent 1",
                "oldest@test.tn",
                "Enfant 1",
                StatutDemandeTransport.EN_ATTENTE,
                LocalDate.now().minusDays(3),
                false
        );
        saveDemande(
                "Parent 2",
                "accepted@test.tn",
                "Enfant 2",
                StatutDemandeTransport.ACCEPTEE,
                LocalDate.now().minusDays(2),
                false
        );
        DemandeTransport latestPending = saveDemande(
                "Parent 3",
                "latest@test.tn",
                "Enfant 3",
                StatutDemandeTransport.EN_ATTENTE,
                LocalDate.now().minusDays(1),
                true
        );

        List<DemandeTransport> pendingDemandes =
                demandeTransportRepository.findDemandesByStatutOrderByOldestFirst(StatutDemandeTransport.EN_ATTENTE);

        assertEquals(2, pendingDemandes.size());
        assertEquals(oldestPending.getId(), pendingDemandes.get(0).getId());
        assertEquals(latestPending.getId(), pendingDemandes.get(1).getId());
    }

    @Test
    void shouldCountOnlySuspiciousPendingDemandes() {
        saveDemande(
                "Parent 4",
                "pending-safe@test.tn",
                "Enfant 4",
                StatutDemandeTransport.EN_ATTENTE,
                LocalDate.now().minusDays(2),
                false
        );
        saveDemande(
                "Parent 5",
                "pending-suspicious@test.tn",
                "Enfant 5",
                StatutDemandeTransport.EN_ATTENTE,
                LocalDate.now().minusDays(1),
                true
        );
        saveDemande(
                "Parent 6",
                "refused-suspicious@test.tn",
                "Enfant 6",
                StatutDemandeTransport.REFUSEE,
                LocalDate.now(),
                true
        );

        long suspiciousPendingCount =
                demandeTransportRepository.countSuspiciousDemandesByStatut(StatutDemandeTransport.EN_ATTENTE);

        assertEquals(1, suspiciousPendingCount);
    }

    private DemandeTransport saveDemande(String parentName,
                                         String email,
                                         String childFirstName,
                                         StatutDemandeTransport statut,
                                         LocalDate dateDemande,
                                         boolean suspicious) {
        User parent = userRepository.save(new User(parentName, email, "secret", true, parentRole));
        Enfant enfant = enfantRepository.save(new Enfant("Doe", childFirstName, parent));

        DemandeTransport demande = new DemandeTransport();
        demande.setParent(parent);
        demande.setEnfant(enfant);
        demande.setStatut(statut);
        demande.setDateDemande(dateDemande);
        demande.setPointRamassage("Ariana");
        demande.setDestinationSouhaitee("Garderie");
        demande.setSensTrajet(SensTrajetDemandeTransport.MAISON_VERS_GARDERIE);
        demande.setAdresseMaison("Ariana centre");
        demande.setLatitudeMaison(36.8);
        demande.setLongitudeMaison(10.1);
        demande.setDateSouhaitee(LocalDate.now().plusDays(1));
        demande.setHeureSouhaitee(LocalTime.of(7, 30));
        demande.setSuspicious(suspicious);
        demande.setAiAnalysisAvailable(true);
        demande.setDuplicateDetected(false);

        return demandeTransportRepository.save(demande);
    }
}
