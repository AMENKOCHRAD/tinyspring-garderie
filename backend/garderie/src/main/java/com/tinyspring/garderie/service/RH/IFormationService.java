package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.entity.RH.AnimatriceFormation;
import com.tinyspring.garderie.entity.RH.Formation;

import java.util.List;
import java.util.Map;

public interface IFormationService {

    // ===== CRUD =====

    Formation creerFormation(Formation formation);

    List<Formation> getToutesFormations();

    Formation getFormationById(Long id);

    Formation modifierFormation(Long id, Formation formation);

    void supprimerFormation(Long id);

    // ===== CYCLE DE VIE =====

    Formation demarrerFormation(Long id);

    Formation demarrerAutomatique(Long id);

    Formation terminerFormation(Long id);

    Formation annulerFormation(Long id, String motif);

    // ===== INSCRIPTIONS =====

    AnimatriceFormation inscrireAnimatrice(Long formationId, Long animatriceId);

    void desinscrireAnimatrice(Long formationId, Long animatriceId);

    // ===== PROFIL & STATS =====

    Map<String, Object> getProfilFormations(Long animatriceId);

    List<Map<String, Object>> getSuggestions(Long animatriceId);

    Map<String, Object> getAlertesGlobales();

    Map<String, Object> getAlertesAnimatrice(Long animatriceId);

    Map<String, Object> getStatsFormations();
}
