package com.tinyspring.garderie.service;

import com.tinyspring.garderie.entity.ObservationEnfant;

public interface ObservationDuplicateDetectionService {

    record Result(boolean duplicate, double score) {}

    Result predictDuplicate(ObservationEnfant newObs, ObservationEnfant existingObs);
}

