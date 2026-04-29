package com.tinyspring.garderie.dto.transport.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

public record AiDetectAnomalyRequest(
        @JsonProperty("child_id")
        Long childId,
        @JsonProperty("request_date")
        LocalDate requestDate,
        @JsonProperty("requested_hour")
        Integer requestedHour,
        @JsonProperty("pickup_address")
        String pickupAddress,
        @JsonProperty("dropoff_address")
        String dropoffAddress,
        @JsonProperty("seats_requested")
        Integer seatsRequested,
        @JsonProperty("route_distance_km")
        Double routeDistanceKm,
        @JsonProperty("is_round_trip")
        boolean roundTrip,
        @JsonProperty("recent_requests")
        List<AiExistingRequestDto> recentRequests
) {
}
