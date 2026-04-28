package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.AiActivityRequestDto;
import com.tinyspring.garderie.dto.AiReportRequestDto;
import com.tinyspring.garderie.dto.AiResponseDto;
import com.tinyspring.garderie.services.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*") // Permettre l'accès depuis Angular
public class AiController {

    private final AiService aiService;

    @Autowired
    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/report/generate")
    public ResponseEntity<AiResponseDto> generateReport(@RequestBody AiReportRequestDto requestDto) {
        if (requestDto == null || requestDto.getAnimatorNotes() == null) {
            return ResponseEntity.badRequest().body(new AiResponseDto("Les notes de l'éducatrice sont requises."));
        }
        
        String response = aiService.generateDailyReport(requestDto.getChildName(), requestDto.getAnimatorNotes());
        return ResponseEntity.ok(new AiResponseDto(response));
    }

    @PostMapping("/activities/suggest")
    public ResponseEntity<AiResponseDto> suggestActivities(@RequestBody AiActivityRequestDto requestDto) {
        if (requestDto == null || requestDto.getAgeMin() == null || requestDto.getAgeMax() == null) {
            return ResponseEntity.badRequest().body(new AiResponseDto("L'âge minimum et maximum sont requis."));
        }

        String response = aiService.suggestActivities(requestDto.getAgeMin(), requestDto.getAgeMax(), requestDto.getCapacite());
        return ResponseEntity.ok(new AiResponseDto(response));
    }

    // --- ML Avancé : K-Means Clustering ---
    @PostMapping("/ml/cluster-children")
    public ResponseEntity<Map<String, Object>> clusterChildren(@RequestBody Map<String, Object> request) {
        List<Map<String, Object>> children = (List<Map<String, Object>>) request.get("children");
        int numGroups = (int) request.get("numGroups");
        Map<String, Object> result = aiService.clusterChildren(children, numGroups);
        return ResponseEntity.ok(result);
    }

    // --- ML Avancé : Decision Tree (Recommandation Salle - basé sur room_dataset.csv) ---
    @PostMapping("/ml/recommend-room")
    public ResponseEntity<Map<String, Object>> recommendRoom(@RequestBody Map<String, Object> request) {
        Integer capacite = (Integer) request.get("capacite");
        Integer ageMoyen = (Integer) request.get("ageMoyen");
        Map<String, Object> result = aiService.recommendRoomForGroup(capacite, ageMoyen);
        return ResponseEntity.ok(result);
    }
}

