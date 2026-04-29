package com.tinyspring.garderie.controller.transport;

import com.tinyspring.garderie.dto.transport.CreateDemandeTransportRequest;
import com.tinyspring.garderie.dto.transport.DemandeTransportResponse;
import com.tinyspring.garderie.dto.transport.UpdateDemandeTransportRequest;
import com.tinyspring.garderie.dto.transport.parent.ParentEnfantResponse;
import com.tinyspring.garderie.dto.transport.parent.ParentTrajetResponse;
import com.tinyspring.garderie.entity.Role;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.transport.SensTrajetDemandeTransport;
import com.tinyspring.garderie.entity.transport.StatutDemandeTransport;
import com.tinyspring.garderie.repository.UserRepository;
import com.tinyspring.garderie.service.transport.TransportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParentTransportApiControllerTest {

    @Mock
    private TransportService transportService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ParentTransportApiController parentTransportApiController;

    @Test
    void shouldReturnParentDemandes() {
        User parent = buildParent();
        when(authentication.getName()).thenReturn("parent@test.tn");
        when(userRepository.findByEmail("parent@test.tn")).thenReturn(Optional.of(parent));
        when(transportService.listerDemandesParParent(12L)).thenReturn(List.of(buildDemandeResponse(51L)));

        List<DemandeTransportResponse> response = parentTransportApiController.getParentDemandes(authentication);

        assertEquals(1, response.size());
        assertEquals(51L, response.get(0).id());
    }

    @Test
    void shouldCreateDemandeForCurrentParent() {
        User parent = buildParent();
        CreateDemandeTransportRequest request = new CreateDemandeTransportRequest();
        request.setEnfantId(2L);
        request.setSensTrajet(SensTrajetDemandeTransport.MAISON_VERS_GARDERIE);
        request.setAdresseMaison("Ariana");
        request.setDateSouhaitee(LocalDate.of(2026, 5, 8));
        request.setHeureSouhaitee(LocalTime.of(7, 45));

        when(authentication.getName()).thenReturn("parent@test.tn");
        when(userRepository.findByEmail("parent@test.tn")).thenReturn(Optional.of(parent));
        when(transportService.creerDemandeTransport(12L, request)).thenReturn(buildDemandeResponse(52L));

        DemandeTransportResponse response = parentTransportApiController.createDemande(request, authentication);

        assertEquals(52L, response.id());
    }

    @Test
    void shouldUpdateDemandeForCurrentParent() {
        User parent = buildParent();
        UpdateDemandeTransportRequest request = new UpdateDemandeTransportRequest();
        request.setEnfantId(2L);
        request.setSensTrajet(SensTrajetDemandeTransport.GARDERIE_VERS_MAISON);
        request.setAdresseMaison("Ariana Nord");
        request.setDateSouhaitee(LocalDate.of(2026, 5, 9));
        request.setHeureSouhaitee(LocalTime.of(16, 30));

        when(authentication.getName()).thenReturn("parent@test.tn");
        when(userRepository.findByEmail("parent@test.tn")).thenReturn(Optional.of(parent));
        when(transportService.modifierDemandeTransport(12L, 53L, request)).thenReturn(buildDemandeResponse(53L));

        DemandeTransportResponse response = parentTransportApiController.updateDemande(53L, request, authentication);

        assertEquals(53L, response.id());
    }

    @Test
    void shouldDeleteDemandeForCurrentParent() {
        User parent = buildParent();
        when(authentication.getName()).thenReturn("parent@test.tn");
        when(userRepository.findByEmail("parent@test.tn")).thenReturn(Optional.of(parent));

        parentTransportApiController.deleteDemande(54L, authentication);

        verify(transportService).supprimerDemandeTransport(12L, 54L);
    }

    @Test
    void shouldReturnAvailableTrajets() {
        ParentTrajetResponse expected = new ParentTrajetResponse(
                61L,
                "Ariana",
                "Garderie",
                LocalDate.of(2026, 5, 10),
                LocalTime.of(8, 0)
        );
        when(transportService.listerTrajetsDisponibles()).thenReturn(List.of(expected));

        List<ParentTrajetResponse> response = parentTransportApiController.getTrajets();

        assertEquals(List.of(expected), response);
    }

    @Test
    void shouldReturnParentChildren() {
        User parent = buildParent();
        ParentEnfantResponse expected = new ParentEnfantResponse(2L, "Lina Test");

        when(authentication.getName()).thenReturn("parent@test.tn");
        when(userRepository.findByEmail("parent@test.tn")).thenReturn(Optional.of(parent));
        when(transportService.listerEnfantsParParent(12L)).thenReturn(List.of(expected));

        List<ParentEnfantResponse> response = parentTransportApiController.getEnfants(authentication);

        assertEquals(List.of(expected), response);
    }

    private User buildParent() {
        User parent = new User("Parent Test", "parent@test.tn", "secret", true, new Role(RoleName.PARENT));
        setId(parent, 12L);
        return parent;
    }

    private DemandeTransportResponse buildDemandeResponse(Long id) {
        return new DemandeTransportResponse(
                id,
                2L,
                "Lina Test",
                12L,
                "Parent Test",
                4L,
                LocalDate.of(2026, 5, 1),
                "Maison",
                "Garderie",
                LocalDate.of(2026, 5, 2),
                LocalTime.of(8, 0),
                StatutDemandeTransport.EN_ATTENTE,
                "Maison",
                "Garderie",
                SensTrajetDemandeTransport.MAISON_VERS_GARDERIE,
                "Ariana",
                36.8,
                10.1,
                "Garderie",
                LocalDate.of(2026, 5, 2),
                LocalTime.of(7, 45),
                false,
                0.2,
                "LOW",
                List.of("RAS"),
                false,
                true,
                "model-v1",
                null,
                null,
                LocalDateTime.of(2026, 5, 1, 9, 0)
        );
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
