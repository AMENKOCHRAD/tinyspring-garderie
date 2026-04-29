package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.ObservationAiRequestDto;
import com.tinyspring.garderie.dto.ObservationAiResponseDto;

public interface ObservationAiService {

    ObservationAiResponseDto generer(ObservationAiRequestDto req);
}

