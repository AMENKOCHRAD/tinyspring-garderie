package com.tinyspring.garderie.dto.transport.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record AiPredictDemandRequest(
        @JsonProperty("target_date")
        LocalDate targetDate,
        @JsonProperty("hour")
        Integer hour,
        @JsonProperty("active_children_count")
        Long activeChildrenCount,
        @JsonProperty("available_transport_count")
        Long availableTransportCount,
        @JsonProperty("avg_route_distance_km")
        Double avgRouteDistanceKm,
        @JsonProperty("rain_flag")
        boolean rainFlag,
        @JsonProperty("school_break_flag")
        boolean schoolBreakFlag
) {
}
