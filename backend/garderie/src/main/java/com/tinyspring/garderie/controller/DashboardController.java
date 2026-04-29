package com.tinyspring.garderie.controller;

import com.tinyspring.garderie.dto.DashboardStatsDTO;
import com.tinyspring.garderie.services.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/kpis")
    public DashboardStatsDTO getKpis() {
        return dashboardService.getStats();
    }
}
