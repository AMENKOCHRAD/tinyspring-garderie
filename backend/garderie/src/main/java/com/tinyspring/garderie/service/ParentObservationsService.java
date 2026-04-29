package com.tinyspring.garderie.service;

import com.tinyspring.garderie.entity.ObservationEnfant;

import java.util.List;

public interface ParentObservationsService {

    List<ObservationEnfant> listerObservationsPourParent(String emailParent, Long enfantId);

    List<ObservationEnfant> listerObservationsParent(String emailParent, boolean unreadOnly);

    long compterNonLues(String emailParent);

    ObservationEnfant marquerLue(String emailParent, Long observationId);
}

