package com.tinyspring.garderie.dto.transport.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiDetectAnomalyResponse(
        @JsonProperty("is_anomaly")
        boolean anomaly,
        @JsonProperty("anomaly_score")
        Double anomalyScore,
        @JsonProperty("anomaly_level")
        String anomalyLevel,
        @JsonProperty("reasons")
        List<String> reasons,
        @JsonProperty("duplicate_found")
        boolean duplicateFound,
        @JsonProperty("model_version")
        String modelVersion
) {
}
