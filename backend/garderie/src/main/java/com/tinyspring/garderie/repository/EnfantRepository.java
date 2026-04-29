package com.tinyspring.garderie.repository;

import com.tinyspring.garderie.entity.Enfant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EnfantRepository extends JpaRepository<Enfant, Long> {

    List<Enfant> findByParentId(Long parentId);

    List<Enfant> findByArchiveFalse();

    boolean existsByParentIdAndNomIgnoreCaseAndPrenomIgnoreCaseAndDateNaissanceAndArchiveFalse(
            Long parentId, String nom, String prenom, LocalDate dateNaissance);

    boolean existsByParentIdAndNomIgnoreCaseAndPrenomIgnoreCaseAndDateNaissanceAndArchiveFalseAndIdNot(
            Long parentId, String nom, String prenom, LocalDate dateNaissance, Long id);
}
