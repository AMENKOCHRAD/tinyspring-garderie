package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.ObservationAiRequestDto;
import com.tinyspring.garderie.dto.ObservationAiResponseDto;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.NiveauUrgence;
import com.tinyspring.garderie.entity.ObservationEnfant;
import com.tinyspring.garderie.entity.ObservationType;
import com.tinyspring.garderie.repository.EnfantRepository;
import com.tinyspring.garderie.repository.ObservationEnfantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObservationAiServiceImplTest {

    @Mock
    EnfantRepository enfantRepository;

    @Mock
    ObservationEnfantRepository observationEnfantRepository;

    @Mock
    OpenAiResponsesClient openAiClient;

    @InjectMocks
    ObservationAiServiceImpl service;

    @Test
    void generer_refuseSiTitreVide() {
        ObservationAiRequestDto req = new ObservationAiRequestDto();
        req.setEnfantId(1L);
        req.setType(ObservationType.SANTE);
        req.setTitre("  ");

        Enfant enfant = new Enfant();
        enfant.setNom("Nom");
        enfant.setPrenom("Prenom");
        when(enfantRepository.findById(1L)).thenReturn(Optional.of(enfant));

        assertThatThrownBy(() -> service.generer(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Titre obligatoire");
    }

    @Test
    void generer_fallbackLocal_sante_remplitChampsEssentiels() {
        ObservationAiRequestDto req = new ObservationAiRequestDto();
        req.setEnfantId(1L);
        req.setType(ObservationType.SANTE);
        req.setTitre("Fièvre");
        req.setUrgence(NiveauUrgence.ELEVEE);
        req.setTemperature(38.7);
        req.setLieu("Salle");
        req.setSymptomes("toux et nez");
        req.setActionsEffectuees("repos");
        req.setContexte("apres repas");

        Enfant enfant = new Enfant();
        enfant.setNom("Nom");
        enfant.setPrenom("Prenom");
        enfant.setAllergies("Arachides");
        when(enfantRepository.findById(1L)).thenReturn(Optional.of(enfant));
        when(observationEnfantRepository.findTop50ByEnfantIdOrderByCreeLeDesc(1L)).thenReturn(List.of());
        when(openAiClient.isEnabled()).thenReturn(false);

        ObservationAiResponseDto out = service.generer(req);

        assertThat(out.getResume()).contains("Fièvre");
        assertThat(out.getDescription()).contains("Symptomes observes");
        assertThat(out.getProblemesPossibles()).isNotEmpty();
        assertThat(out.getMessageParent()).isNotBlank();
    }

    @Test
    void generer_fallbackLocal_sommeil_inclutContexteRecent() {
        ObservationAiRequestDto req = new ObservationAiRequestDto();
        req.setEnfantId(2L);
        req.setType(ObservationType.SOMMEIL);
        req.setTitre("Sommeil agite");
        req.setSymptomes("reveils frequents");

        Enfant enfant = new Enfant();
        enfant.setNom("Nom");
        enfant.setPrenom("Prenom");
        when(enfantRepository.findById(2L)).thenReturn(Optional.of(enfant));

        ObservationEnfant recent = new ObservationEnfant();
        recent.setType(ObservationType.HUMEUR);
        recent.setTitre("Irritable");
        when(observationEnfantRepository.findTop50ByEnfantIdOrderByCreeLeDesc(2L)).thenReturn(List.of(recent));
        when(openAiClient.isEnabled()).thenReturn(false);

        ObservationAiResponseDto out = service.generer(req);

        assertThat(out.getContexteRecent()).isNotEmpty();
        assertThat(out.getDescription()).contains("Etat du sommeil");
    }
}
