package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.RiskPredictionDto;
import com.tinyspring.garderie.service.RiskPredictionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"})
@RestController
@RequestMapping("/api/admin/risques")
public class AdminRiskController {

    private final RiskPredictionService riskPredictionService;

    public AdminRiskController(RiskPredictionService riskPredictionService) {
        this.riskPredictionService = riskPredictionService;
    }

    @GetMapping("/enfant/{enfantId}")
    public ResponseEntity<RiskPredictionDto> predireRisque(@PathVariable Long enfantId) {
        return ResponseEntity.ok(riskPredictionService.predirePourEnfant(enfantId));
    }
}

