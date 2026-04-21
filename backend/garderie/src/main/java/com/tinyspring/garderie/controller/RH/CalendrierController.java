package com.tinyspring.garderie.controller.RH;

import com.tinyspring.garderie.dto.RH.CalendrierEventDTO;
import com.tinyspring.garderie.entity.RH.AbsenceConge;
import com.tinyspring.garderie.repository.RH.AbsenceCongeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/calendrier")
@RequiredArgsConstructor
public class CalendrierController {

    private final AbsenceCongeRepository absenceCongeRepository;
    // ✅ FormationRepository supprimé — sera rajouté avec la nouvelle logique

    @GetMapping("/events")
    public ResponseEntity<List<CalendrierEventDTO>> getEvents() {
        List<CalendrierEventDTO> events = new ArrayList<>();

        // Absences & Congés
        for (AbsenceConge ac : absenceCongeRepository.findAll()) {
            String color = switch (ac.getStatut()) {
                case APPROUVE -> "#10b981";
                case REFUSE   -> "#ef4444";
                default       -> "#f59e0b";
            };

            String titre = ac.getAnimatrice().getPrenom() + " "
                    + ac.getAnimatrice().getNom()
                    + " — " + ac.getType().name().replace("_", " ");

            events.add(CalendrierEventDTO.builder()
                    .id("abs-" + ac.getId())
                    .title(titre)
                    .start(ac.getDateDebut().toString())
                    .end(ac.getDateFin().plusDays(1).toString())
                    .color(color)
                    .type("ABSENCE")
                    .build());
        }

        // ✅ Formations supprimées temporairement — seront rajoutées avec la nouvelle logique

        return ResponseEntity.ok(events);
    }
}