package com.tinyspring.garderie.service.transport.impl;

import com.tinyspring.garderie.dto.transport.CreateDemandeTransportRequest;
import com.tinyspring.garderie.dto.transport.DemandeTransportResponse;
import com.tinyspring.garderie.dto.transport.TraitementDemandeTransportResponse;
import com.tinyspring.garderie.dto.transport.UpdateDemandeTransportRequest;
import com.tinyspring.garderie.entity.Role;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.transport.AffectationTransport;
import com.tinyspring.garderie.entity.transport.DemandeTransport;
import com.tinyspring.garderie.entity.transport.Enfant;
import com.tinyspring.garderie.entity.transport.SensTrajetDemandeTransport;
import com.tinyspring.garderie.entity.transport.StatutDemandeTransport;
import com.tinyspring.garderie.entity.transport.Trajet;
import com.tinyspring.garderie.entity.transport.Transport;
import com.tinyspring.garderie.exception.BusinessException;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.repository.transport.AffectationTransportRepository;
import com.tinyspring.garderie.repository.transport.DemandeTransportRepository;
import com.tinyspring.garderie.repository.transport.EnfantRepository;
import com.tinyspring.garderie.repository.transport.TrajetRepository;
import com.tinyspring.garderie.repository.transport.TransportRepository;
import com.tinyspring.garderie.service.transport.ai.AiAnomalyAnalysisResult;
import com.tinyspring.garderie.service.transport.ai.TransportAiService;
import com.tinyspring.garderie.service.transport.recommendation.TransportRecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransportServiceImplTest {

    @Mock
    private DemandeTransportRepository demandeTransportRepository;

    @Mock
    private AffectationTransportRepository affectationTransportRepository;

    @Mock
    private EnfantRepository enfantRepository;

    @Mock
    private TransportRepository transportRepository;

    @Mock
    private TrajetRepository trajetRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransportRecommendationService transportRecommendationService;

    @Mock
    private TransportAiService transportAiService;

    @InjectMocks
    private TransportServiceImpl transportService;

    @BeforeEach
    void setUp() {
        lenient().when(demandeTransportRepository.save(any(DemandeTransport.class))).thenAnswer(invocation -> {
            DemandeTransport demande = invocation.getArgument(0);
            if (demande.getId() == null) {
                setId(demande, 501L);
            }
            return demande;
        });
        lenient().when(affectationTransportRepository.save(any(AffectationTransport.class))).thenAnswer(invocation -> {
            AffectationTransport affectation = invocation.getArgument(0);
            if (affectation.getId() == null) {
                setId(affectation, 801L);
            }
            return affectation;
        });
    }

    @Test
    void shouldCreateDemandeTransportForParent() {
        User parent = buildParent(11L);
        Enfant enfant = buildEnfant(21L, parent, "Yasmine");
        CreateDemandeTransportRequest request = buildCreateRequest();

        when(userRepository.findById(11L)).thenReturn(Optional.of(parent));
        when(enfantRepository.findByIdAndParentId(21L, 11L)).thenReturn(Optional.of(enfant));
        when(demandeTransportRepository.existsByEnfantIdAndStatut(21L, StatutDemandeTransport.EN_ATTENTE)).thenReturn(false);
        when(demandeTransportRepository.existsByEnfantIdAndStatut(21L, StatutDemandeTransport.REVISION_PARENT_DEMANDEE)).thenReturn(false);
        when(demandeTransportRepository.existsByEnfantIdAndStatut(21L, StatutDemandeTransport.ACCEPTEE)).thenReturn(false);
        when(transportAiService.analyserDemande(any(DemandeTransport.class), eq(null))).thenReturn(new AiAnomalyAnalysisResult(
                true,
                false,
                0.15,
                "LOW",
                List.of("Adresse coherente"),
                false,
                "model-v1",
                null
        ));

        DemandeTransportResponse response = transportService.creerDemandeTransport(11L, request);

        assertEquals(501L, response.id());
        assertEquals(StatutDemandeTransport.EN_ATTENTE, response.statut());
        assertTrue(response.aiAnalysisAvailable());
        assertFalse(response.suspicious());
        assertEquals("Ariana 1000", response.pointRamassage());
        assertEquals("15 Rue des Ecoles, El Menzah 5, Ariana 2091, Tunisie", response.destination());
    }

    @Test
    void shouldRejectCreateDemandeWhenUserIsNotParent() {
        User admin = new User("Admin", "admin@test.tn", "secret", true, new Role(RoleName.ADMIN));
        setId(admin, 99L);
        CreateDemandeTransportRequest request = buildCreateRequest();

        when(userRepository.findById(99L)).thenReturn(Optional.of(admin));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> transportService.creerDemandeTransport(99L, request)
        );

        assertEquals("Seul un parent peut effectuer une demande de transport", exception.getMessage());
    }

    @Test
    void shouldModifyDemandeAndResetRevisionState() {
        User parent = buildParent(12L);
        Enfant enfant = buildEnfant(22L, parent, "Adam");
        DemandeTransport demande = buildDemande(502L, parent, enfant, StatutDemandeTransport.REVISION_PARENT_DEMANDEE);
        demande.setRevisionRequestMessage("Ancien message");
        demande.setRevisionRequestedAt(LocalDateTime.now().minusDays(1));
        UpdateDemandeTransportRequest request = buildUpdateRequest();

        when(userRepository.findById(12L)).thenReturn(Optional.of(parent));
        when(demandeTransportRepository.findById(502L)).thenReturn(Optional.of(demande));
        when(enfantRepository.findByIdAndParentId(22L, 12L)).thenReturn(Optional.of(enfant));
        when(transportAiService.analyserDemande(any(DemandeTransport.class), eq(502L))).thenReturn(new AiAnomalyAnalysisResult(
                true,
                false,
                0.20,
                "LOW",
                List.of("Mise a jour correcte"),
                false,
                "model-v2",
                null
        ));

        DemandeTransportResponse response = transportService.modifierDemandeTransport(12L, 502L, request);

        assertEquals(StatutDemandeTransport.EN_ATTENTE, response.statut());
        assertNull(response.revisionRequestMessage());
        assertNull(response.revisionRequestedAt());
        assertEquals(LocalTime.of(16, 30), response.heureSouhaitee());
        assertEquals("15 Rue des Ecoles, El Menzah 5, Ariana 2091, Tunisie", response.pointRamassage());
    }

    @Test
    void shouldMoveSuspiciousDemandeToRevisionWhenAccepting() {
        User parent = buildParent(13L);
        Enfant enfant = buildEnfant(23L, parent, "Sami");
        DemandeTransport demande = buildDemande(503L, parent, enfant, StatutDemandeTransport.EN_ATTENTE);
        demande.setSuspicious(true);
        demande.setAnomalyReasons("Adresse identique | Heure inhabituelle");

        when(demandeTransportRepository.findById(503L)).thenReturn(Optional.of(demande));

        TraitementDemandeTransportResponse response = transportService.accepterDemande(503L);

        assertEquals(StatutDemandeTransport.REVISION_PARENT_DEMANDEE, response.statut());
        assertNull(response.affectationId());
        assertTrue(demande.getRevisionRequestMessage().contains("Motif: Adresse identique | Heure inhabituelle"));
        verify(affectationTransportRepository, never()).save(any(AffectationTransport.class));
    }

    @Test
    void shouldAcceptDemandeAndCreateAffectationWhenNotSuspicious() {
        User parent = buildParent(14L);
        Enfant enfant = buildEnfant(24L, parent, "Lina");
        DemandeTransport demande = buildDemande(504L, parent, enfant, StatutDemandeTransport.EN_ATTENTE);
        Transport transport = new Transport("Bus A", "TN-101", 10);
        setId(transport, 301L);
        Trajet trajet = new Trajet("Ariana", "Garderie", LocalDate.now().plusDays(1), LocalTime.of(8, 0), transport);
        setId(trajet, 401L);

        when(demandeTransportRepository.findById(504L)).thenReturn(Optional.of(demande));
        when(affectationTransportRepository.existsByEnfantId(24L)).thenReturn(false);
        when(transportRecommendationService.validerEtRetournerTrajetPourAffectation(demande)).thenReturn(trajet);
        when(transportRepository.findById(301L)).thenReturn(Optional.of(transport));
        when(affectationTransportRepository.countByTransportId(301L)).thenReturn(1L);

        TraitementDemandeTransportResponse response = transportService.accepterDemande(504L);

        assertEquals(StatutDemandeTransport.ACCEPTEE, response.statut());
        assertEquals(801L, response.affectationId());
        assertEquals(10.0, response.tauxRemplissageTransport());
        assertEquals(trajet, demande.getTrajet());
    }

    @Test
    void shouldRefusePendingDemande() {
        User parent = buildParent(15L);
        Enfant enfant = buildEnfant(25L, parent, "Aya");
        DemandeTransport demande = buildDemande(505L, parent, enfant, StatutDemandeTransport.EN_ATTENTE);

        when(demandeTransportRepository.findById(505L)).thenReturn(Optional.of(demande));

        TraitementDemandeTransportResponse response = transportService.refuserDemande(505L);

        assertEquals(StatutDemandeTransport.REFUSEE, response.statut());
        assertEquals("Demande refusee.", response.message());
    }

    @Test
    void shouldCalculateTauxRemplissage() {
        Transport transport = new Transport("Bus B", "TN-102", 8);
        setId(transport, 302L);

        when(transportRepository.findById(302L)).thenReturn(Optional.of(transport));
        when(affectationTransportRepository.countByTransportId(302L)).thenReturn(2L);

        double tauxRemplissage = transportService.calculerTauxRemplissage(302L);

        assertEquals(25.0, tauxRemplissage);
    }

    @Test
    void shouldThrowWhenTransportCapacityIsInvalid() {
        Transport transport = new Transport("Bus C", "TN-103", 0);
        setId(transport, 303L);

        when(transportRepository.findById(303L)).thenReturn(Optional.of(transport));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> transportService.calculerTauxRemplissage(303L)
        );

        assertEquals("La capacite du transport doit etre superieure a zero", exception.getMessage());
    }

    private CreateDemandeTransportRequest buildCreateRequest() {
        CreateDemandeTransportRequest request = new CreateDemandeTransportRequest();
        request.setEnfantId(21L);
        request.setSensTrajet(SensTrajetDemandeTransport.MAISON_VERS_GARDERIE);
        request.setAdresseMaison("  Ariana 1000  ");
        request.setLatitudeMaison(36.80);
        request.setLongitudeMaison(10.18);
        request.setDateSouhaitee(LocalDate.now().plusDays(2));
        request.setHeureSouhaitee(LocalTime.of(7, 45));
        return request;
    }

    private UpdateDemandeTransportRequest buildUpdateRequest() {
        UpdateDemandeTransportRequest request = new UpdateDemandeTransportRequest();
        request.setEnfantId(22L);
        request.setSensTrajet(SensTrajetDemandeTransport.GARDERIE_VERS_MAISON);
        request.setAdresseMaison(" Nouvelle Ariana ");
        request.setLatitudeMaison(36.81);
        request.setLongitudeMaison(10.19);
        request.setDateSouhaitee(LocalDate.now().plusDays(3));
        request.setHeureSouhaitee(null);
        return request;
    }

    private User buildParent(Long id) {
        User parent = new User("Parent Test", "parent" + id + "@test.tn", "secret", true, new Role(RoleName.PARENT));
        setId(parent, id);
        return parent;
    }

    private Enfant buildEnfant(Long id, User parent, String prenom) {
        Enfant enfant = new Enfant("Doe", prenom, parent);
        setId(enfant, id);
        return enfant;
    }

    private DemandeTransport buildDemande(Long id, User parent, Enfant enfant, StatutDemandeTransport statut) {
        DemandeTransport demande = new DemandeTransport();
        setId(demande, id);
        demande.setParent(parent);
        demande.setEnfant(enfant);
        demande.setStatut(statut);
        demande.setPointRamassage("Ariana");
        demande.setDestinationSouhaitee("Garderie");
        demande.setSensTrajet(SensTrajetDemandeTransport.MAISON_VERS_GARDERIE);
        demande.setAdresseMaison("Ariana");
        demande.setLatitudeMaison(36.8);
        demande.setLongitudeMaison(10.1);
        demande.setDateDemande(LocalDate.now().minusDays(1));
        demande.setDateSouhaitee(LocalDate.now().plusDays(1));
        demande.setHeureSouhaitee(LocalTime.of(7, 30));
        demande.setAiAnalysisAvailable(true);
        demande.setDuplicateDetected(false);
        return demande;
    }

    private void setId(Object target, Long id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
