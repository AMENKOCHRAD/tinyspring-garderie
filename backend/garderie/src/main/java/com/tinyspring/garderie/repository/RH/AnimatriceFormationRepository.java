package com.tinyspring.garderie.repository.RH;

import com.tinyspring.garderie.entity.RH.AnimatriceFormation;
import com.tinyspring.garderie.entity.RH.enums.StatutInscription;
import com.tinyspring.garderie.entity.RH.enums.StatutValidite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnimatriceFormationRepository extends JpaRepository<AnimatriceFormation, Long> {

    List<AnimatriceFormation> findByAnimatriceId(Long animatriceId);

    List<AnimatriceFormation> findByFormationId(Long formationId);

    Optional<AnimatriceFormation> findByAnimatriceIdAndFormationId(Long animatriceId, Long formationId);

    boolean existsByAnimatriceIdAndFormationId(Long animatriceId, Long formationId);

    List<AnimatriceFormation> findByStatut(StatutInscription statut);

    List<AnimatriceFormation> findByAnimatriceIdAndStatut(Long animatriceId, StatutInscription statut);

    List<AnimatriceFormation> findByAnimatriceIdAndStatutOrderByDateCompletionDesc(
            Long animatriceId, StatutInscription statut);

    List<AnimatriceFormation> findByStatutValidite(StatutValidite statutValidite);

    List<AnimatriceFormation> findByAnimatriceIdAndStatutValidite(
            Long animatriceId, StatutValidite statutValidite);

    @Query("SELECT af.formation.id FROM AnimatriceFormation af WHERE af.animatrice.id = :animatriceId AND af.statut = 'TERMINEE'")
    List<Long> findFormationIdsSuiviesByAnimatriceId(Long animatriceId);

    List<AnimatriceFormation> findByFormationIdAndStatutOrderByDateInscriptionAsc(
            Long formationId, StatutInscription statut);

    @Query("SELECT COUNT(af) FROM AnimatriceFormation af WHERE af.animatrice.id = :animatriceId AND af.statut = 'TERMINEE'")
    long countFormationsTermineesByAnimatrice(Long animatriceId);

    @Query("SELECT DISTINCT af.animatrice.id FROM AnimatriceFormation af WHERE af.statut = 'TERMINEE' AND af.dateCompletion > :since")
    List<Long> findAnimatriceIdsAvecFormationRecente(LocalDate since);

    @Query("SELECT af FROM AnimatriceFormation af WHERE af.animatrice.id = :animatriceId AND af.statutValidite = 'EXPIREE'")
    List<AnimatriceFormation> findFormationsExpireesByAnimatrice(Long animatriceId);

    @Query("SELECT COUNT(af) FROM AnimatriceFormation af WHERE af.formation.id = :formationId AND af.statut = 'INSCRITE'")
    long countInscritsActifs(Long formationId);

    // ✅ NOUVEAU — Supprimer toutes les inscriptions d'une formation
    void deleteByFormationId(Long formationId);
}