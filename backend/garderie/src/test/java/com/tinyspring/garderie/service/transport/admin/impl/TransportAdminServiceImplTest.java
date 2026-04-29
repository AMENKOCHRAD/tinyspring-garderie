package com.tinyspring.garderie.service.transport.admin.impl;

import com.tinyspring.garderie.dto.transport.admin.AdminDemandPredictionResponse;
import com.tinyspring.garderie.dto.transport.admin.AffectationTransportResponse;
import com.tinyspring.garderie.dto.transport.admin.TrajetRequest;
import com.tinyspring.garderie.dto.transport.admin.TrajetResponse;
import com.tinyspring.garderie.dto.transport.admin.TransportRequest;
import com.tinyspring.garderie.dto.transport.admin.TransportResponse;
import com.tinyspring.garderie.entity.Role;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.transport.AffectationTransport;
import com.tinyspring.garderie.entity.transport.Enfant;
import com.tinyspring.garderie.entity.transport.Trajet;
import com.tinyspring.garderie.entity.transport.Transport;
import com.tinyspring.garderie.exception.BusinessException;
import com.tinyspring.garderie.repository.transport.AffectationTransportRepository;
import com.tinyspring.garderie.repository.transport.DemandeTransportRepository;
import com.tinyspring.garderie.repository.transport.TrajetRepository;
import com.tinyspring.garderie.repository.transport.TransportRepository;
import com.tinyspring.garderie.service.transport.TransportService;
import com.tinyspring.garderie.service.transport.recommendation.TransportRecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransportAdminServiceImplTest {

    @Mock
    private TransportRepository transportRepository;

    @Mock
    private TrajetRepository trajetRepository;

    @Mock
    private AffectationTransportRepository affectationTransportRepository;

    @Mock
    private DemandeTransportRepository demandeTransportRepository;

    @Mock
    private TransportService transportService;

    @Mock
    private TransportRecommendationService transportRecommendationService;

    @InjectMocks
    private TransportAdminServiceImpl transportAdminService;

    @BeforeEach
    void setUp() {
        lenient().when(transportRepository.save(any(Transport.class))).thenAnswer(invocation -> {
            Transport transport = invocation.getArgument(0);
            if (transport.getId() == null) {
                setId(transport, 100L);
            }
            return transport;
        });

        lenient().when(trajetRepository.save(any(Trajet.class))).thenAnswer(invocation -> {
            Trajet trajet = invocation.getArgument(0);
            if (trajet.getId() == null) {
                setId(trajet, 200L);
            }
            return trajet;
        });
    }

    @Test
    void shouldCreateTransport() {
        TransportRequest request = buildTransportRequest("Mini Bus", "TN-501", 14);
        when(transportService.calculerTauxRemplissage(100L)).thenReturn(25.0);

        TransportResponse response = transportAdminService.creerTransport(request);

        assertEquals(100L, response.id());
        assertEquals("Mini Bus", response.nom());
        assertEquals("TN-501", response.matricule());
        assertEquals(14, response.capacite());
        assertEquals(25.0, response.tauxRemplissage());
    }

    @Test
    void shouldUpdateTransport() {
        Transport transport = new Transport("Ancien Bus", "TN-050", 10);
        setId(transport, 101L);
        TransportRequest request = buildTransportRequest("Bus Renove", "TN-051", 18);

        when(transportRepository.findById(101L)).thenReturn(Optional.of(transport));
        when(transportService.calculerTauxRemplissage(101L)).thenReturn(50.0);

        TransportResponse response = transportAdminService.modifierTransport(101L, request);

        assertEquals(101L, response.id());
        assertEquals("Bus Renove", response.nom());
        assertEquals("TN-051", response.matricule());
        assertEquals(18, response.capacite());
        assertEquals(50.0, response.tauxRemplissage());
    }

    @Test
    void shouldBlockTransportDeletionWhenAffectationsExist() {
        when(affectationTransportRepository.countByTransportId(101L)).thenReturn(2L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> transportAdminService.supprimerTransport(101L)
        );

        assertEquals("Impossible de supprimer un transport deja utilise dans une affectation", exception.getMessage());
        verify(transportRepository, never()).delete(any(Transport.class));
    }

    @Test
    void shouldBlockTransportDeletionWhenTrajetAssociated() {
        Transport transport = new Transport("Bus A", "TN-111", 12);
        setId(transport, 101L);
        Trajet trajet = new Trajet("Ariana", "Garderie", LocalDate.now().plusDays(1), LocalTime.of(8, 0), transport);
        setId(trajet, 201L);

        when(affectationTransportRepository.countByTransportId(101L)).thenReturn(0L);
        when(trajetRepository.findAll()).thenReturn(List.of(trajet));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> transportAdminService.supprimerTransport(101L)
        );

        assertEquals("Impossible de supprimer un transport deja associe a un trajet", exception.getMessage());
        verify(transportRepository, never()).delete(any(Transport.class));
    }

    @Test
    void shouldDeleteTransportWhenUnused() {
        Transport transport = new Transport("Bus Libre", "TN-211", 15);
        setId(transport, 102L);

        when(affectationTransportRepository.countByTransportId(102L)).thenReturn(0L);
        when(trajetRepository.findAll()).thenReturn(List.of());
        when(transportRepository.findById(102L)).thenReturn(Optional.of(transport));

        transportAdminService.supprimerTransport(102L);

        verify(transportRepository).delete(transport);
    }

    @Test
    void shouldCreateTrajet() {
        Transport transport = new Transport("Bus C", "TN-300", 20);
        setId(transport, 103L);
        TrajetRequest request = buildTrajetRequest(103L);

        when(transportRepository.findById(103L)).thenReturn(Optional.of(transport));

        TrajetResponse response = transportAdminService.creerTrajet(request);

        assertEquals(200L, response.id());
        assertEquals("Menzah", response.pointDepart());
        assertEquals("Garderie Centrale", response.destination());
        assertEquals("Zone Nord", response.zoneDesservie());
        assertEquals(103L, response.transportId());
        assertEquals("Bus C", response.transportNom());
    }

    @Test
    void shouldUpdateTrajet() {
        Transport oldTransport = new Transport("Bus Old", "TN-310", 10);
        setId(oldTransport, 104L);
        Transport newTransport = new Transport("Bus New", "TN-311", 16);
        setId(newTransport, 105L);
        Trajet trajet = new Trajet("Ancien depart", "Ancienne destination", LocalDate.now().plusDays(1), LocalTime.of(7, 30), oldTransport);
        setId(trajet, 202L);
        TrajetRequest request = buildTrajetRequest(105L);

        when(trajetRepository.findById(202L)).thenReturn(Optional.of(trajet));
        when(transportRepository.findById(105L)).thenReturn(Optional.of(newTransport));

        TrajetResponse response = transportAdminService.modifierTrajet(202L, request);

        assertEquals(202L, response.id());
        assertEquals("Menzah", response.pointDepart());
        assertEquals("Garderie Centrale", response.destination());
        assertEquals(105L, response.transportId());
        assertEquals("Bus New", response.transportNom());
    }

    @Test
    void shouldBlockTrajetDeletionWhenAffectationsExist() {
        AffectationTransport affectation = new AffectationTransport();
        setId(affectation, 601L);

        when(affectationTransportRepository.findByTrajetId(202L)).thenReturn(List.of(affectation));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> transportAdminService.supprimerTrajet(202L)
        );

        assertEquals("Impossible de supprimer un trajet deja utilise dans une affectation", exception.getMessage());
        verify(trajetRepository, never()).delete(any(Trajet.class));
    }

    @Test
    void shouldBlockTrajetDeletionWhenDemandeExists() {
        when(affectationTransportRepository.findByTrajetId(203L)).thenReturn(List.of());
        when(demandeTransportRepository.existsByTrajetId(203L)).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> transportAdminService.supprimerTrajet(203L)
        );

        assertEquals("Impossible de supprimer un trajet deja utilise dans une demande", exception.getMessage());
        verify(trajetRepository, never()).delete(any(Trajet.class));
    }

    @Test
    void shouldDeleteTrajetWhenUnused() {
        Transport transport = new Transport("Bus D", "TN-400", 12);
        setId(transport, 106L);
        Trajet trajet = new Trajet("Lac", "Garderie", LocalDate.now().plusDays(2), LocalTime.of(8, 15), transport);
        setId(trajet, 204L);

        when(affectationTransportRepository.findByTrajetId(204L)).thenReturn(List.of());
        when(demandeTransportRepository.existsByTrajetId(204L)).thenReturn(false);
        when(trajetRepository.findById(204L)).thenReturn(Optional.of(trajet));

        transportAdminService.supprimerTrajet(204L);

        verify(trajetRepository).delete(trajet);
    }

    @Test
    void shouldListAffectations() {
        User parent = new User("Parent", "parent@test.tn", "secret", true, new Role(RoleName.PARENT));
        setId(parent, 1L);
        Enfant enfant = new Enfant("Ben Ali", "Meriem", parent);
        setId(enfant, 2L);
        Transport transport = new Transport("Bus Scolaire", "TN-777", 18);
        setId(transport, 3L);
        Trajet trajet = new Trajet("Ariana", "Garderie", LocalDate.now().plusDays(1), LocalTime.of(7, 45), transport);
        setId(trajet, 4L);
        AffectationTransport affectation = new AffectationTransport(enfant, transport, trajet, "Maison");
        setId(affectation, 5L);

        when(affectationTransportRepository.findAll()).thenReturn(List.of(affectation));

        List<AffectationTransportResponse> responses = transportAdminService.listerAffectations();

        assertEquals(1, responses.size());
        assertEquals("Meriem Ben Ali", responses.get(0).enfantNomComplet());
        assertEquals("Bus Scolaire", responses.get(0).transportNom());
        assertEquals("Maison", responses.get(0).pointRamassage());
    }

    @Test
    void shouldDelegateAdminPrediction() {
        AdminDemandPredictionResponse expected = new AdminDemandPredictionResponse(
                LocalDate.of(2026, 5, 1),
                8,
                35L,
                4L,
                6.2,
                18,
                "MOYENNE",
                true,
                "model-v1",
                "Prediction disponible"
        );

        when(transportService.predireDemandeAdmin(LocalDate.of(2026, 5, 1), 8, true, false)).thenReturn(expected);

        AdminDemandPredictionResponse response = transportAdminService.predireDemandeAdmin(
                LocalDate.of(2026, 5, 1),
                8,
                true,
                false
        );

        assertEquals(expected, response);
    }

    private TransportRequest buildTransportRequest(String nom, String matricule, int capacite) {
        TransportRequest request = new TransportRequest();
        request.setNom(nom);
        request.setMatricule(matricule);
        request.setCapacite(capacite);
        return request;
    }

    private TrajetRequest buildTrajetRequest(Long transportId) {
        TrajetRequest request = new TrajetRequest();
        request.setPointDepart("Menzah");
        request.setDestination("Garderie Centrale");
        request.setDateTrajet(LocalDate.now().plusDays(3));
        request.setHeureDepart(LocalTime.of(8, 30));
        request.setTransportId(transportId);
        request.setZoneDesservie("Zone Nord");
        request.setLatitudeDestination(36.81);
        request.setLongitudeDestination(10.16);
        return request;
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
