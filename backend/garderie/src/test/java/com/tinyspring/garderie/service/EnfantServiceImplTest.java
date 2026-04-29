package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.EnfantDTO;
import com.tinyspring.garderie.dto.EnfantResponseDTO;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.mapper.EnfantMapper;
import com.tinyspring.garderie.repository.EnfantRepository;
import com.tinyspring.garderie.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnfantServiceImplTest {

    @Mock
    EnfantRepository enfantRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    EnfantMapper enfantMapper;

    @InjectMocks
    EnfantServiceImpl service;

    @Test
    void ajouterEnfant_refuseDoublonPourMemeParent_nomPrenomDate() {
        EnfantDTO dto = new EnfantDTO();
        dto.parentId = 10L;
        dto.nom = "Ben Salah";
        dto.prenom = "Lina";
        dto.dateNaissance = "2022-05-01";

        User parent = new User();
        parent.setId(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(enfantRepository.existsByParentIdAndNomIgnoreCaseAndPrenomIgnoreCaseAndDateNaissanceAndArchiveFalse(
                10L, "Ben Salah", "Lina", LocalDate.parse("2022-05-01")
        )).thenReturn(true);

        assertThatThrownBy(() -> service.ajouterEnfant(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("existe deja");

        verify(enfantRepository, never()).save(any());
    }

    @Test
    void ajouterEnfant_creeSiPasDoublon_etForceArchiveFalseEtParent() {
        EnfantDTO dto = new EnfantDTO();
        dto.parentId = 7L;
        dto.nom = "Ben Salah";
        dto.prenom = "Adam";
        dto.dateNaissance = "2021-02-10";

        User parent = new User();
        parent.setId(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(parent));
        when(enfantRepository.existsByParentIdAndNomIgnoreCaseAndPrenomIgnoreCaseAndDateNaissanceAndArchiveFalse(
                eq(7L), anyString(), anyString(), any(LocalDate.class)
        )).thenReturn(false);

        Enfant mapped = new Enfant();
        when(enfantMapper.toEntity(dto)).thenReturn(mapped);

        when(enfantRepository.save(any(Enfant.class))).thenAnswer(inv -> inv.getArgument(0, Enfant.class));

        Enfant saved = service.ajouterEnfant(dto);

        assertThat(saved).isSameAs(mapped);
        assertThat(saved.getParent()).isSameAs(parent);
        assertThat(saved.isArchive()).isFalse();
        verify(enfantMapper).toEntity(dto);

        ArgumentCaptor<Enfant> captor = ArgumentCaptor.forClass(Enfant.class);
        verify(enfantRepository).save(captor.capture());
        assertThat(captor.getValue().getParent()).isSameAs(parent);
    }

    @Test
    void modifierEnfant_refuseDoublonSurAutreEnfantDuMemeParent() {
        EnfantDTO dto = new EnfantDTO();
        dto.parentId = 99L;
        dto.nom = "Test";
        dto.prenom = "Kid";
        dto.dateNaissance = "2020-01-01";

        Enfant existing = new Enfant();
        existing.setId(123L);
        when(enfantRepository.findById(123L)).thenReturn(Optional.of(existing));

        User parent = new User();
        parent.setId(99L);
        when(userRepository.findById(99L)).thenReturn(Optional.of(parent));

        when(enfantRepository.existsByParentIdAndNomIgnoreCaseAndPrenomIgnoreCaseAndDateNaissanceAndArchiveFalseAndIdNot(
                99L, "Test", "Kid", LocalDate.parse("2020-01-01"), 123L
        )).thenReturn(true);

        assertThatThrownBy(() -> service.modifierEnfant(123L, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("existe deja");

        verify(enfantMapper, never()).updateEntityFromDto(any(), any());
        verify(enfantRepository, never()).save(any());
    }

    @Test
    void getAllEnfants_utiliseMapperResponse() {
        Enfant e1 = new Enfant();
        Enfant e2 = new Enfant();
        when(enfantRepository.findByArchiveFalse()).thenReturn(java.util.List.of(e1, e2));

        when(enfantMapper.toResponseDto(any())).thenReturn(new EnfantResponseDTO());

        assertThat(service.getAllEnfants()).hasSize(2);
        verify(enfantMapper, times(2)).toResponseDto(any());
    }

    @Test
    void archiverEnfant_metArchiveTrueEtSauvegarde() {
        Enfant enfant = new Enfant();
        enfant.setId(5L);
        enfant.setArchive(false);
        when(enfantRepository.findById(5L)).thenReturn(Optional.of(enfant));
        when(enfantRepository.save(any(Enfant.class))).thenAnswer(inv -> inv.getArgument(0, Enfant.class));

        Enfant saved = service.archiverEnfant(5L);

        assertThat(saved.isArchive()).isTrue();
        verify(enfantRepository).save(enfant);
    }

    @Test
    void modifierEnfant_metAJourEtSauvegarde_siPasDoublon() {
        EnfantDTO dto = new EnfantDTO();
        dto.parentId = 1L;
        dto.nom = "Nom";
        dto.prenom = "Prenom";
        dto.dateNaissance = "2020-01-01";

        Enfant enfant = new Enfant();
        enfant.setId(9L);
        when(enfantRepository.findById(9L)).thenReturn(Optional.of(enfant));

        User parent = new User();
        parent.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(parent));

        when(enfantRepository.existsByParentIdAndNomIgnoreCaseAndPrenomIgnoreCaseAndDateNaissanceAndArchiveFalseAndIdNot(
                1L, "Nom", "Prenom", LocalDate.parse("2020-01-01"), 9L
        )).thenReturn(false);

        when(enfantRepository.save(any(Enfant.class))).thenAnswer(inv -> inv.getArgument(0, Enfant.class));

        Enfant saved = service.modifierEnfant(9L, dto);

        assertThat(saved.getParent()).isSameAs(parent);
        verify(enfantMapper).updateEntityFromDto(dto, enfant);
        verify(enfantRepository).save(enfant);
    }
}
