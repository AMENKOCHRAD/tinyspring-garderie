package com.tinyspring.garderie.dto.transport.admin;

import java.time.LocalDate;

public record AdminDemandPredictionResponse(
        LocalDate targetDate,
        Integer hour,
        Long activeChildrenCount,
        Long availableTransportCount,
        Double avgRouteDistanceKm,
        Integer predictedDemandCount,
        String demandLevel,
        boolean aiAvailable,
        String modelVersion,
        String message
) {
}
