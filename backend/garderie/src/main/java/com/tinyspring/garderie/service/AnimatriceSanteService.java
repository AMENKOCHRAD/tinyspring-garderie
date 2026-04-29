package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.ObservationCreateDto;
import com.tinyspring.garderie.dto.PriseTraitementCreateDto;
import com.tinyspring.garderie.entity.ObservationEnfant;
import com.tinyspring.garderie.entity.PriseTraitement;

import java.time.LocalDate;
import java.util.List;

public interface AnimatriceSanteService {

    PriseTraitement enregistrerPrise(String emailAnimatrice, Long traitementId, PriseTraitementCreateDto payload);

    List<PriseTraitement> listerPrisesParEnfant(Long enfantId, LocalDate date);

    List<PriseTraitement> listerPrisesParEnfantPeriode(Long enfantId, LocalDate from, LocalDate to);

    List<PriseTraitement> listerToutesPrises(String emailAnimatrice, LocalDate date);

    List<PriseTraitement> listerMesPrises(String emailAnimatrice, LocalDate from, LocalDate to);

    ObservationEnfant creerObservation(String emailAnimatrice, Long enfantId, ObservationCreateDto payload);

    byte[] genererPdfPrise(String emailAnimatrice, Long priseId);

    List<ObservationEnfant> listerObservationsEnfant(Long enfantId);

    List<ObservationEnfant> listerDernieresObservations();
}

