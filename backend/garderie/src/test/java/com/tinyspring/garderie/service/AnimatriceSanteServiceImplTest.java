package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.ObservationCreateDto;
import com.tinyspring.garderie.dto.PriseTraitementCreateDto;
import com.tinyspring.garderie.entity.*;
import com.tinyspring.garderie.repository.EnfantRepository;
import com.tinyspring.garderie.repository.ObservationEnfantRepository;
import com.tinyspring.garderie.repository.PriseTraitementRepository;
import com.tinyspring.garderie.repository.TraitementRepository;
import com.tinyspring.garderie.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnimatriceSanteServiceImplTest {

    @Mock
    TraitementRepository traitementRepository;

    @Mock
    EnfantRepository enfantRepository;

    @Mock
    PriseTraitementRepository priseTraitementRepository;

    @Mock
    ObservationEnfantRepository observationEnfantRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    PriseTraitementPdfService priseTraitementPdfService;

    @Mock
    RealtimeNotificationService realtimeNotificationService;

    @Mock
    ObservationDuplicateDetectionService duplicateDetectionService;

    @InjectMocks
    AnimatriceSanteServiceImpl service;

    @Test
    void enregistrerPrise_refuseLeDimanche() {
        when(userRepository.findByEmailIgnoreCase("a@test.com")).thenReturn(Optional.of(user("a@test.com")));
        Traitement traitement = new Traitement();
        traitement.setStatut(StatutTraitement.VALIDE);
        traitement.setHeuresPrises(List.of("09:00"));
        when(traitementRepository.findById(1L)).thenReturn(Optional.of(traitement));

        PriseTraitementCreateDto dto = new PriseTraitementCreateDto();
        dto.setDatePrise(LocalDate.of(2026, 5, 3)); // Sunday
        dto.setHeurePrevue("09:00");

        assertThatThrownBy(() -> service.enregistrerPrise("a@test.com", 1L, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("dimanche");
    }

    @Test
    void enregistrerPrise_refuseSamediApres1230() {
        when(userRepository.findByEmailIgnoreCase("a@test.com")).thenReturn(Optional.of(user("a@test.com")));
        Traitement traitement = new Traitement();
        traitement.setStatut(StatutTraitement.VALIDE);
        traitement.setHeuresPrises(List.of("13:00"));
        when(traitementRepository.findById(1L)).thenReturn(Optional.of(traitement));

        PriseTraitementCreateDto dto = new PriseTraitementCreateDto();
        dto.setDatePrise(LocalDate.of(2026, 5, 2)); // Saturday
        dto.setHeurePrevue("13:00");

        assertThatThrownBy(() -> service.enregistrerPrise("a@test.com", 1L, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("12:30");
    }

    @Test
    void enregistrerPrise_refuseSiHeurePasDansTraitement() {
        when(userRepository.findByEmailIgnoreCase("a@test.com")).thenReturn(Optional.of(user("a@test.com")));
        Traitement traitement = new Traitement();
        traitement.setStatut(StatutTraitement.VALIDE);
        traitement.setHeuresPrises(List.of("09:00"));
        when(traitementRepository.findById(1L)).thenReturn(Optional.of(traitement));

        PriseTraitementCreateDto dto = new PriseTraitementCreateDto();
        dto.setDatePrise(LocalDate.of(2026, 4, 29)); // Wednesday
        dto.setHeurePrevue("10:00");

        assertThatThrownBy(() -> service.enregistrerPrise("a@test.com", 1L, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Heure prevue invalide");
    }

    @Test
    void creerObservation_detecteDoublon_etLanceException() {
        when(userRepository.findByEmailIgnoreCase("a@test.com")).thenReturn(Optional.of(user("a@test.com")));

        Enfant enfant = new Enfant();
        User parent = new User();
        parent.setEmail("p@test.com");
        enfant.setParent(parent);
        when(enfantRepository.findById(2L)).thenReturn(Optional.of(enfant));

        ObservationEnfant existing = new ObservationEnfant();
        existing.setEnfant(enfant);
        existing.setCreeLe(LocalDateTime.now());
        // pas de setter id -> Mockito va voir getId == null, donc on mock via spy
        ObservationEnfant existingSpy = spy(existing);
        when(existingSpy.getId()).thenReturn(100L);

        when(observationEnfantRepository.findByEnfantIdAndCreeLeBetweenOrderByCreeLeDesc(eq(2L), any(), any()))
                .thenReturn(List.of(existingSpy));

        when(duplicateDetectionService.predictDuplicate(any(), eq(existingSpy)))
                .thenReturn(new ObservationDuplicateDetectionService.Result(true, 0.92));

        ObservationCreateDto payload = new ObservationCreateDto();
        payload.setType(ObservationType.SANTE);
        payload.setTitre("T");
        payload.setDescription("D");
        payload.setForceCreate(false);

        assertThatThrownBy(() -> service.creerObservation("a@test.com", 2L, payload))
                .isInstanceOf(DuplicateObservationException.class);

        verify(observationEnfantRepository, never()).save(any());
    }

    @Test
    void creerObservation_forceCreate_ignoreDoublon_etNotifieParent() {
        when(userRepository.findByEmailIgnoreCase("a@test.com")).thenReturn(Optional.of(user("a@test.com")));

        Enfant enfant = new Enfant();
        User parent = new User();
        parent.setEmail("p@test.com");
        enfant.setParent(parent);
        when(enfantRepository.findById(2L)).thenReturn(Optional.of(enfant));

        ObservationCreateDto payload = new ObservationCreateDto();
        payload.setType(ObservationType.SANTE);
        payload.setTitre("T");
        payload.setDescription("D");
        payload.setForceCreate(true);

        when(observationEnfantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ObservationEnfant saved = service.creerObservation("a@test.com", 2L, payload);

        assertThat(saved.getTitre()).isEqualTo("T");
        verify(realtimeNotificationService).notifyParentObservationCreated(eq("p@test.com"), any(ObservationEnfant.class));
    }

    private static User user(String email) {
        User u = new User();
        u.setEmail(email);
        u.setNom("A");
        return u;
    }
}

