package com.tinyspring.garderie.dto.transport.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiPredictDemandResponse(
        @JsonProperty("predicted_demand_count")
        Integer predictedDemandCount,
        @JsonProperty("demand_level")
        String demandLevel,
        @JsonProperty("model_version")
        String modelVersion
) {
}
