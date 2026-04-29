package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.RapportRHDTO;
import com.tinyspring.garderie.dto.RH.mapper.RapportRHMapper;
import com.tinyspring.garderie.entity.RH.AbsenceConge;
import com.tinyspring.garderie.entity.RH.Animatrice;
import com.tinyspring.garderie.entity.RH.RapportRH;
import com.tinyspring.garderie.entity.RH.enums.StatutAbsenceConge;
import com.tinyspring.garderie.entity.RH.enums.StatutAnimatrice;
import com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge;
import com.tinyspring.garderie.repository.RH.AbsenceCongeRepository;
import com.tinyspring.garderie.repository.RH.AnimatriceRepository;
import com.tinyspring.garderie.repository.RH.RapportRHRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests RapportRHServiceImpl")
class RapportRHServiceImplTest {

    @Mock private IOllamaService ollamaService;
    @Mock private RapportRHRepository rapportRHRepository;
    @Mock private AbsenceCongeRepository absenceCongeRepository;
    @Mock private AnimatriceRepository animatriceRepository;
    @Mock private RapportRHMapper rapportRHMapper;

    @InjectMocks private RapportRHServiceImpl service;

    private RapportRH rapport;
    private RapportRHDTO rapportDTO;
    private Animatrice animatrice;
    private AbsenceConge absence;

    @BeforeEach
    void setUp() {
        animatrice = new Animatrice();
        animatrice.setId(1L);
        animatrice.setStatut(StatutAnimatrice.ACTIVE);

        absence = AbsenceConge.builder()
                .animatrice(animatrice)
                .type(TypeAbsenceConge.CONGE_ANNUEL)
                .dateDebut(LocalDate.now())
                .dateFin(LocalDate.now().plusDays(3))
                .statut(StatutAbsenceConge.APPROUVE)
                .nbJours(3)
                .build();
        absence.setId(1L);

        rapport = RapportRH.builder()
                .id(1L)
                .question("Rapport mensuel")
                .typeRapport("MENSUEL")
                .periode("Mois en cours")
                .contenu("Contenu généré")
                .dateGeneration(LocalDateTime.now())
                .build();

        rapportDTO = RapportRHDTO.builder()
                .id(1L)
                .question("Rapport mensuel")
                .typeRapport("MENSUEL")
                .periode("Mois en cours")
                .contenu("Contenu généré")
                .dateGeneration(LocalDateTime.now())
                .build();
    }

    // ===== genererRapport =====

    @Test
    @DisplayName("genererRapport — doit générer et sauvegarder le rapport")
    void genererRapport_doitGenererEtSauvegarder() {
        when(animatriceRepository.count()).thenReturn(3L);
        when(animatriceRepository.findByStatut(StatutAnimatrice.ACTIVE))
                .thenReturn(List.of(animatrice));
        when(absenceCongeRepository.findAll()).thenReturn(List.of(absence));
        when(ollamaService.generer(any())).thenReturn("Rapport généré par Ollama");
        when(rapportRHRepository.save(any())).thenReturn(rapport);
        when(rapportRHMapper.toDTO(any())).thenReturn(rapportDTO);

        RapportRHDTO result = service.genererRapport("Rapport mensuel");

        assertThat(result).isNotNull();
        assertThat(result.getQuestion()).isEqualTo("Rapport mensuel");
        verify(ollamaService).generer(any());
        verify(rapportRHRepository).save(any());
    }

    @Test
    @DisplayName("genererRapport — doit détecter type MENSUEL")
    void genererRapport_doitDetecterTypeMensuel() {
        when(animatriceRepository.count()).thenReturn(2L);
        when(animatriceRepository.findByStatut(any())).thenReturn(List.of());
        when(absenceCongeRepository.findAll()).thenReturn(List.of());
        when(ollamaService.generer(any())).thenReturn("Réponse");
        when(rapportRHRepository.save(any())).thenAnswer(inv -> {
            RapportRH r = inv.getArgument(0);
            assertThat(r.getTypeRapport()).isEqualTo("MENSUEL");
            return rapport;
        });
        when(rapportRHMapper.toDTO(any())).thenReturn(rapportDTO);

        service.genererRapport("Rapport du mois de janvier");

        verify(rapportRHRepository).save(any());
    }

    @Test
    @DisplayName("genererRapport — doit détecter type ABSENCES")
    void genererRapport_doitDetecterTypeAbsences() {
        when(animatriceRepository.count()).thenReturn(2L);
        when(animatriceRepository.findByStatut(any())).thenReturn(List.of());
        when(absenceCongeRepository.findAll()).thenReturn(List.of());
        when(ollamaService.generer(any())).thenReturn("Réponse");
        when(rapportRHRepository.save(any())).thenAnswer(inv -> {
            RapportRH r = inv.getArgument(0);
            assertThat(r.getTypeRapport()).isEqualTo("ABSENCES");
            return rapport;
        });
        when(rapportRHMapper.toDTO(any())).thenReturn(rapportDTO);

        service.genererRapport("Statistiques des absences");

        verify(rapportRHRepository).save(any());
    }

    @Test
    @DisplayName("genererRapport — doit détecter type FORMATIONS")
    void genererRapport_doitDetecterTypeFormations() {
        when(animatriceRepository.count()).thenReturn(2L);
        when(animatriceRepository.findByStatut(any())).thenReturn(List.of());
        when(absenceCongeRepository.findAll()).thenReturn(List.of());
        when(ollamaService.generer(any())).thenReturn("Réponse");
        when(rapportRHRepository.save(any())).thenAnswer(inv -> {
            RapportRH r = inv.getArgument(0);
            assertThat(r.getTypeRapport()).isEqualTo("FORMATIONS");
            return rapport;
        });
        when(rapportRHMapper.toDTO(any())).thenReturn(rapportDTO);

        service.genererRapport("Liste des formations disponibles");

        verify(rapportRHRepository).save(any());
    }

    @Test
    @DisplayName("genererRapport — doit détecter période par nom de mois")
    void genererRapport_doitDetecterPeriodeMois() {
        when(animatriceRepository.count()).thenReturn(2L);
        when(animatriceRepository.findByStatut(any())).thenReturn(List.of());
        when(absenceCongeRepository.findAll()).thenReturn(List.of());
        when(ollamaService.generer(any())).thenReturn("Réponse");
        when(rapportRHRepository.save(any())).thenAnswer(inv -> {
            RapportRH r = inv.getArgument(0);
            assertThat(r.getPeriode()).isEqualTo("Janvier");
            return rapport;
        });
        when(rapportRHMapper.toDTO(any())).thenReturn(rapportDTO);

        service.genererRapport("Rapport pour janvier");

        verify(rapportRHRepository).save(any());
    }

    @Test
    @DisplayName("genererRapport — doit détecter type ANIMATRICES")
    void genererRapport_doitDetecterTypeAnimatrices() {
        when(animatriceRepository.count()).thenReturn(2L);
        when(animatriceRepository.findByStatut(any())).thenReturn(List.of());
        when(absenceCongeRepository.findAll()).thenReturn(List.of());
        when(ollamaService.generer(any())).thenReturn("Réponse");
        when(rapportRHRepository.save(any())).thenAnswer(inv -> {
            RapportRH r = inv.getArgument(0);
            assertThat(r.getTypeRapport()).isEqualTo("ANIMATRICES");
            return rapport;
        });
        when(rapportRHMapper.toDTO(any())).thenReturn(rapportDTO);

        service.genererRapport("Bilan des animatrices");

        verify(rapportRHRepository).save(any());
    }

    @Test
    @DisplayName("genererRapport — doit détecter type GENERAL si aucun mot-clé")
    void genererRapport_doitDetecterTypeGeneral() {
        when(animatriceRepository.count()).thenReturn(2L);
        when(animatriceRepository.findByStatut(any())).thenReturn(List.of());
        when(absenceCongeRepository.findAll()).thenReturn(List.of());
        when(ollamaService.generer(any())).thenReturn("Réponse");
        when(rapportRHRepository.save(any())).thenAnswer(inv -> {
            RapportRH r = inv.getArgument(0);
            assertThat(r.getTypeRapport()).isEqualTo("GENERAL");
            return rapport;
        });
        when(rapportRHMapper.toDTO(any())).thenReturn(rapportDTO);

        service.genererRapport("Bilan général");

        verify(rapportRHRepository).save(any());
    }

    // ===== getTousLesRapports =====

    @Test
    @DisplayName("getTousLesRapports — doit retourner tous les rapports")
    void getTousLesRapports_doitRetournerTousRapports() {
        when(rapportRHRepository.findAllByOrderByDateGenerationDesc())
                .thenReturn(List.of(rapport));
        when(rapportRHMapper.toDTO(any())).thenReturn(rapportDTO);

        List<RapportRHDTO> result = service.getTousLesRapports();

        assertThat(result).hasSize(1);
        verify(rapportRHRepository).findAllByOrderByDateGenerationDesc();
    }

    // ===== getRapportById =====

    @Test
    @DisplayName("getRapportById — doit retourner le rapport si trouvé")
    void getRapportById_doitRetournerRapport() {
        when(rapportRHRepository.findById(1L)).thenReturn(Optional.of(rapport));
        when(rapportRHMapper.toDTO(any())).thenReturn(rapportDTO);

        RapportRHDTO result = service.getRapportById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getRapportById — doit lever exception si introuvable")
    void getRapportById_doitLeverException() {
        when(rapportRHRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRapportById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }
}