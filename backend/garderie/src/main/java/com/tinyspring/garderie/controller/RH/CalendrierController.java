package com.tinyspring.garderie.controller.RH;

import com.tinyspring.garderie.dto.RH.CalendrierEventDTO;
import com.tinyspring.garderie.entity.RH.AbsenceConge;
import com.tinyspring.garderie.entity.RH.Formation;
import com.tinyspring.garderie.repository.RH.AbsenceCongeRepository;
import com.tinyspring.garderie.repository.RH.FormationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/calendrier")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201"})
public class CalendrierController {

    private final AbsenceCongeRepository absenceCongeRepository;
    private final FormationRepository formationRepository;

    @GetMapping("/events")
    public ResponseEntity<List<CalendrierEventDTO>> getEvents() {
        List<CalendrierEventDTO> events = new ArrayList<>();

        // ===== Absences =====
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

        // ===== Formations =====
        for (Formation f : formationRepository.findAll()) {
            if (f.getDateDebut() != null) {
                events.add(CalendrierEventDTO.builder()
                        .id("form-" + f.getId())
                        .title("📚 " + f.getTitre())
                        .start(f.getDateDebut().toString())
                        .end(f.getDateFin() != null
                                ? f.getDateFin().plusDays(1).toString()
                                : f.getDateDebut().plusDays(1).toString())
                        .color("#6366f1")
                        .type("FORMATION")
                        .build());
            }
        }

        return ResponseEntity.ok(events);
    }
}