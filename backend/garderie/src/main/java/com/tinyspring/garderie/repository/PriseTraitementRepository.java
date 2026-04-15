package com.tinyspring.garderie.repository;

import com.tinyspring.garderie.entity.PriseTraitement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriseTraitementRepository extends JpaRepository<PriseTraitement, Long> {
    Optional<PriseTraitement> findByTraitementIdAndDatePriseAndHeurePrevue(Long traitementId, LocalDate datePrise, String heurePrevue);

    List<PriseTraitement> findByTraitementConditionSanitaireEnfantIdAndDatePrise(Long enfantId, LocalDate datePrise);
}

