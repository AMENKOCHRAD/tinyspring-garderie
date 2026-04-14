package com.tinyspring.garderie.dto.transport.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record AiExistingRequestDto(
        @JsonProperty("child_id")
        Long childId,
        @JsonProperty("request_date")
        LocalDate requestDate,
        @JsonProperty("requested_hour")
        Integer requestedHour,
        @JsonProperty("pickup_address")
        String pickupAddress,
        @JsonProperty("dropoff_address")
        String dropoffAddress
) {
}
