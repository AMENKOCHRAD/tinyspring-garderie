package com.tinyspring.garderie.service.transport.ai;

import java.util.List;

public record AiAnomalyAnalysisResult(
        boolean aiAvailable,
        boolean suspicious,
        Double anomalyScore,
        String anomalyLevel,
        List<String> anomalyReasons,
        boolean duplicateFound,
        String modelVersion,
        String errorMessage
) {
}
