package com.tinyspring.garderie.service.transport.ai;

import com.tinyspring.garderie.dto.transport.admin.AdminDemandPredictionResponse;
import com.tinyspring.garderie.entity.transport.DemandeTransport;

import java.time.LocalDate;

public interface TransportAiService {
    AiAnomalyAnalysisResult analyserDemande(DemandeTransport demande, Long currentDemandeId);

    AdminDemandPredictionResponse predireDemandePourDashboard(LocalDate targetDate,
                                                              Integer hour,
                                                              boolean rainFlag,
                                                              boolean schoolBreakFlag);
}
