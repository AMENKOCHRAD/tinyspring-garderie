package com.tinyspring.garderie.repository.RH;

import com.tinyspring.garderie.entity.RH.Formation;
import com.tinyspring.garderie.entity.RH.enums.StatutFormation;
import com.tinyspring.garderie.entity.RH.enums.TypeFormation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FormationRepository extends JpaRepository<Formation, Long> {

    List<Formation> findByType(TypeFormation type);

    List<Formation> findByStatutInscription(StatutFormation statut);

    List<Formation> findByFormateur(String formateur);

    List<Formation> findByAnimatricesId(Long animatriceId);
}