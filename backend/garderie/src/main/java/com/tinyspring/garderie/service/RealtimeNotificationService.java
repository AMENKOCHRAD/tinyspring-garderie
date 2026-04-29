package com.tinyspring.garderie.service;

import com.tinyspring.garderie.entity.ObservationEnfant;

public interface RealtimeNotificationService {

    void notifyParentObservationCreated(String parentEmail, ObservationEnfant observation);

    void notifyParentUnreadCountChanged(String parentEmail);

    void notifyAnimatricesDoseDue(Long enfantId,
                                  String enfantNom,
                                  String enfantPrenom,
                                  Long traitementId,
                                  String nomTraitement,
                                  String dateIso,
                                  String heurePrevue);

    void notifyParentTraitementValidation(String parentEmail,
                                          Long enfantId,
                                          Long traitementId,
                                          String nomTraitement,
                                          String statut,
                                          String note);
}

