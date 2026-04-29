package com.tinyspring.garderie.controller.RH;

import com.tinyspring.garderie.dto.RH.CalendrierEventDTO;
import com.tinyspring.garderie.entity.RH.AbsenceConge;
import com.tinyspring.garderie.entity.RH.Animatrice;
import com.tinyspring.garderie.entity.RH.enums.StatutAbsenceConge;
import com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge;
import com.tinyspring.garderie.repository.RH.AbsenceCongeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests CalendrierController")
class CalendrierControllerTest {

    @Mock  private AbsenceCongeRepository absenceCongeRepository;
    @InjectMocks private CalendrierController controller;

    private Animatrice animatrice;

    @BeforeEach
    void setUp() {
        animatrice = new Animatrice();
        animatrice.setId(1L); animatrice.setNom("Ben Ali"); animatrice.setPrenom("Sara");
    }

    private AbsenceConge buildAbsence(StatutAbsenceConge statut) {
        AbsenceConge ac = AbsenceConge.builder()
                .animatrice(animatrice)
                .type(TypeAbsenceConge.CONGE_ANNUEL)
                .dateDebut(LocalDate.now())
                .dateFin(LocalDate.now().plusDays(5))
                .statut(statut).nbJours(5).build();
        ac.setId(1L);
        return ac;
    }

    @Test @DisplayName("getEvents — 200 avec absence APPROUVE en vert")
    void getEvents_approuve() {
        when(absenceCongeRepository.findAll()).thenReturn(List.of(buildAbsence(StatutAbsenceConge.APPROUVE)));
        ResponseEntity<List<CalendrierEventDTO>> r = controller.getEvents();
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).hasSize(1);
        assertThat(r.getBody().get(0).getColor()).isEqualTo("#10b981");
        assertThat(r.getBody().get(0).getType()).isEqualTo("ABSENCE");
    }

    @Test @DisplayName("getEvents — absence REFUSE en rouge")
    void getEvents_refuse() {
        when(absenceCongeRepository.findAll()).thenReturn(List.of(buildAbsence(StatutAbsenceConge.REFUSE)));
        ResponseEntity<List<CalendrierEventDTO>> r = controller.getEvents();
        assertThat(r.getBody().get(0).getColor()).isEqualTo("#ef4444");
    }

    @Test @DisplayName("getEvents — absence EN_ATTENTE en orange")
    void getEvents_enAttente() {
        when(absenceCongeRepository.findAll()).thenReturn(List.of(buildAbsence(StatutAbsenceConge.EN_ATTENTE)));
        ResponseEntity<List<CalendrierEventDTO>> r = controller.getEvents();
        assertThat(r.getBody().get(0).getColor()).isEqualTo("#f59e0b");
    }

    @Test @DisplayName("getEvents — 200 liste vide si aucune absence")
    void getEvents_vide() {
        when(absenceCongeRepository.findAll()).thenReturn(List.of());
        ResponseEntity<List<CalendrierEventDTO>> r = controller.getEvents();
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getBody()).isEmpty();
    }

    @Test @DisplayName("getEvents — titre contient nom et type")
    void getEvents_titre() {
        when(absenceCongeRepository.findAll()).thenReturn(List.of(buildAbsence(StatutAbsenceConge.APPROUVE)));
        ResponseEntity<List<CalendrierEventDTO>> r = controller.getEvents();
        String titre = r.getBody().get(0).getTitle();
        assertThat(titre).contains("Sara").contains("Ben Ali").contains("CONGE ANNUEL");
    }
}
