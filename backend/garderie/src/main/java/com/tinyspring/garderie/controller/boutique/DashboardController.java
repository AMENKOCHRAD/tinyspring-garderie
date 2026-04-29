package com.tinyspring.garderie.controller.boutique;

import com.tinyspring.garderie.dto.boutique.DashboardStatsDto;
import com.tinyspring.garderie.service.boutique.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController("boutiqueDashboardController")
@RequestMapping("/api/admin/boutique/dashboard")
@CrossOrigin(origins = "http://localhost:4200")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }
}
