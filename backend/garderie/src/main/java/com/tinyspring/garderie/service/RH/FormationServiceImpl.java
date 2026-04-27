package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.entity.RH.Animatrice;
import com.tinyspring.garderie.entity.RH.AnimatriceFormation;
import com.tinyspring.garderie.entity.RH.Formation;
import com.tinyspring.garderie.entity.RH.enums.*;
import com.tinyspring.garderie.repository.RH.AnimatriceFormationRepository;
import com.tinyspring.garderie.repository.RH.AnimatriceRepository;
import com.tinyspring.garderie.repository.RH.FormationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormationServiceImpl implements IFormationService {

    private final FormationRepository formationRepository;
    private final AnimatriceFormationRepository animatriceFormationRepository;
    private final AnimatriceRepository animatriceRepository;
    private final INotificationService notificationService;

    // ===== CRUD =====

    @Override
    public Formation creerFormation(Formation formation) {
        formation.setStatut(StatutFormation.OUVERTE);
        Formation saved = formationRepository.save(formation);
        notifierNouvelleFormation(saved);
        if (Boolean.TRUE.equals(formation.getObligatoire())) {
            inscrireToutes(saved);
        }
        return saved;
    }

    @Override
    public List<Formation> getToutesFormations() {
        return formationRepository.findAll();
    }

    @Override
    public Formation getFormationById(Long id) {
        return formationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Formation non trouvée : " + id));
    }

    @Override
    public Formation modifierFormation(Long id, Formation formation) {
        Formation existing = getFormationById(id);
        existing.setTitre(formation.getTitre());
        existing.setDescription(formation.getDescription());
        existing.setType(formation.getType());
        existing.setFormateur(formation.getFormateur());
        existing.setLieu(formation.getLieu());
        existing.setDateFormation(formation.getDateFormation());
        existing.setHeureDebut(formation.getHeureDebut());
        existing.setHeureFin(formation.getHeureFin());
        existing.setPlacesMax(formation.getPlacesMax());
        existing.setDureeValiditeMois(formation.getDureeValiditeMois());
        existing.setObligatoire(formation.getObligatoire());
        return formationRepository.save(existing);
    }

    @Override
    @Transactional
    public void supprimerFormation(Long id) {
        animatriceFormationRepository.deleteByFormationId(id);
        formationRepository.deleteById(id);
    }

    // ===== CYCLE DE VIE =====

    @Override
    @Transactional
    public Formation demarrerFormation(Long id) {
        Formation formation = getFormationById(id);
        if (formation.getStatut() != StatutFormation.OUVERTE)
            throw new RuntimeException("La formation doit être OUVERTE pour être démarrée");
        if (formation.getNbInscrits() == 0)
            throw new RuntimeException("Impossible de démarrer : aucune animatrice inscrite");

        formation.setStatut(StatutFormation.EN_COURS);
        Formation saved = formationRepository.save(formation);

        animatriceFormationRepository.findByFormationId(id).stream()
                .filter(i -> StatutInscription.INSCRITE.equals(i.getStatut()))
                .forEach(i -> notificationService.creerNotification(
                        "🚀 La formation '" + formation.getTitre() + "' vient de commencer !",
                        "FORMATION_DEMARREE"
                ));
        return saved;
    }

    @Override
    @Transactional
    public Formation demarrerAutomatique(Long id) {
        Formation formation = getFormationById(id);
        if (formation.getStatut() != StatutFormation.OUVERTE) return formation;

        formation.setStatut(StatutFormation.EN_COURS);
        Formation saved = formationRepository.save(formation);

        animatriceFormationRepository.findByFormationId(id).stream()
                .filter(i -> StatutInscription.INSCRITE.equals(i.getStatut()))
                .forEach(i -> notificationService.creerNotification(
                        "🚀 La formation '" + formation.getTitre() + "' vient de commencer !",
                        "FORMATION_DEMARREE"
                ));
        return saved;
    }

    @Override
    @Transactional
    public Formation terminerFormation(Long id) {
        Formation formation = getFormationById(id);
        if (formation.getStatut() != StatutFormation.EN_COURS)
            throw new RuntimeException("La formation doit être EN_COURS pour être terminée");

        formation.setStatut(StatutFormation.TERMINEE);
        formationRepository.save(formation);

        List<AnimatriceFormation> inscriptions = animatriceFormationRepository.findByFormationId(id);
        LocalDate dateCompletion = LocalDate.now();
        int nbCertificats = 0;

        for (AnimatriceFormation inscription : inscriptions) {
            if (StatutInscription.INSCRITE.equals(inscription.getStatut())) {
                inscription.setStatut(StatutInscription.TERMINEE);
                inscription.setDateCompletion(dateCompletion);
                if (formation.getDureeValiditeMois() != null) {
                    inscription.setDateExpiration(dateCompletion.plusMonths(formation.getDureeValiditeMois()));
                }
                inscription.setCertificationGeneree(true);
                animatriceFormationRepository.save(inscription);
                nbCertificats++;
                notificationService.creerNotification(
                        "🎓 Félicitations ! Vous avez complété '" + formation.getTitre() +
                                "'. Votre certificat est disponible.",
                        "FORMATION_TERMINEE"
                );
            }
        }

        notificationService.creerNotification(
                "✅ Formation '" + formation.getTitre() + "' terminée — " +
                        nbCertificats + " certificat(s) générés",
                "FORMATION_TERMINEE_ADMIN"
        );
        return formation;
    }

    @Override
    @Transactional
    public Formation annulerFormation(Long id, String motif) {
        Formation formation = getFormationById(id);
        if (formation.getStatut() == StatutFormation.TERMINEE)
            throw new RuntimeException("Impossible d'annuler une formation terminée");

        formation.setStatut(StatutFormation.ANNULEE);
        formationRepository.save(formation);

        animatriceFormationRepository.findByFormationId(id).stream()
                .filter(i -> StatutInscription.INSCRITE.equals(i.getStatut()))
                .forEach(i -> {
                    i.setStatut(StatutInscription.ABANDONNEE);
                    animatriceFormationRepository.save(i);
                    notificationService.creerNotification(
                            "❌ La formation '" + formation.getTitre() +
                                    "' a été annulée. Motif : " + motif,
                            "FORMATION_ANNULEE"
                    );
                });
        return formation;
    }

    // ===== INSCRIPTIONS =====

    @Override
    @Transactional
    public AnimatriceFormation inscrireAnimatrice(Long formationId, Long animatriceId) {
        Formation formation = getFormationById(formationId);
        animatriceRepository.findById(animatriceId)
                .orElseThrow(() -> new RuntimeException("Animatrice non trouvée"));

        if (formation.getStatut() != StatutFormation.OUVERTE)
            throw new RuntimeException("🚫 La formation n'est plus ouverte aux inscriptions");

        if (animatriceFormationRepository.existsByAnimatriceIdAndFormationId(animatriceId, formationId))
            throw new RuntimeException("🚫 Vous êtes déjà inscrite à cette formation");

        StatutInscription statut;
        String messageNotif;

        if (formation.isComplet()) {
            statut = StatutInscription.LISTE_ATTENTE;
            messageNotif = "⏳ Vous êtes en liste d'attente pour '" + formation.getTitre() + "'.";
        } else {
            statut = StatutInscription.INSCRITE;
            messageNotif = "✅ Votre inscription à '" + formation.getTitre() +
                    "' est confirmée ! Date : " + formation.getDateFormation();
        }

        Animatrice animatrice = animatriceRepository.findById(animatriceId).get();
        AnimatriceFormation inscription = AnimatriceFormation.builder()
                .animatrice(animatrice)
                .formation(formation)
                .dateInscription(LocalDate.now())
                .statut(statut)
                .certificationGeneree(false)
                .build();

        AnimatriceFormation saved = animatriceFormationRepository.save(inscription);
        notificationService.creerNotification(messageNotif, "INSCRIPTION_FORMATION");
        return saved;
    }

    @Override
    @Transactional
    public void desinscrireAnimatrice(Long formationId, Long animatriceId) {
        AnimatriceFormation inscription = animatriceFormationRepository
                .findByAnimatriceIdAndFormationId(animatriceId, formationId)
                .orElseThrow(() -> new RuntimeException("Inscription non trouvée"));

        inscription.setStatut(StatutInscription.ABANDONNEE);
        animatriceFormationRepository.save(inscription);
        promouvoirListeAttente(formationId);
    }

    // ===== PROFIL & STATS =====

    @Override
    public Map<String, Object> getProfilFormations(Long animatriceId) {
        Animatrice animatrice = animatriceRepository.findById(animatriceId)
                .orElseThrow(() -> new RuntimeException("Animatrice non trouvée"));

        List<AnimatriceFormation> toutes = animatriceFormationRepository.findByAnimatriceId(animatriceId);
        toutes.forEach(af -> {
            af.calculerStatutValidite();
            animatriceFormationRepository.save(af);
        });

        long terminees  = toutes.stream().filter(af -> StatutInscription.TERMINEE.equals(af.getStatut())).count();
        long inscrites  = toutes.stream().filter(af -> StatutInscription.INSCRITE.equals(af.getStatut())).count();
        long enAttente  = toutes.stream().filter(af -> StatutInscription.LISTE_ATTENTE.equals(af.getStatut())).count();
        long bientotExp = toutes.stream().filter(af -> StatutValidite.BIENTOT_EXPIREE.equals(af.getStatutValidite())).count();
        long expirees   = toutes.stream().filter(af -> StatutValidite.EXPIREE.equals(af.getStatutValidite())).count();

        Map<String, Object> profil = new HashMap<>();
        profil.put("animatrice", Map.of(
                "id", animatrice.getId(),
                "nom", animatrice.getNom(),
                "prenom", animatrice.getPrenom(),
                "specialite", animatrice.getSpecialite() != null ? animatrice.getSpecialite() : ""
        ));
        profil.put("formations", toutes);
        profil.put("stats", Map.of(
                "total", toutes.size(),
                "terminees", terminees,
                "inscrites", inscrites,
                "enAttente", enAttente,
                "bientotExpirees", bientotExp,
                "expirees", expirees,
                "tauxParticipation", toutes.size() > 0 ?
                        Math.round((double) terminees / toutes.size() * 100) : 0
        ));
        profil.put("suggestions", getSuggestions(animatriceId));
        profil.put("alertes", getAlertesAnimatrice(animatriceId));
        return profil;
    }

    @Override
    public List<Map<String, Object>> getSuggestions(Long animatriceId) {
        Animatrice animatrice = animatriceRepository.findById(animatriceId)
                .orElseThrow(() -> new RuntimeException("Animatrice non trouvée"));

        List<Long> formationsSuivies =
                animatriceFormationRepository.findFormationIdsSuiviesByAnimatriceId(animatriceId);
        List<AnimatriceFormation> formationsExpirees =
                animatriceFormationRepository.findFormationsExpireesByAnimatrice(animatriceId);
        List<Formation> toutesFormations = formationRepository.findByStatut(StatutFormation.OUVERTE);

        List<Map<String, Object>> suggestions = new ArrayList<>();

        for (Formation formation : toutesFormations) {
            if (animatriceFormationRepository.existsByAnimatriceIdAndFormationId(
                    animatriceId, formation.getId())) continue;

            String priorite = null;
            String raison = null;

            boolean estRecyclage = formationsExpirees.stream()
                    .anyMatch(af -> af.getFormation().getType().equals(formation.getType()));
            if (estRecyclage) {
                priorite = "URGENT";
                raison = "🔁 Recyclage requis — formation expirée du même type";
            }

            if (priorite == null && Boolean.TRUE.equals(formation.getObligatoire())
                    && !formationsSuivies.contains(formation.getId())) {
                priorite = "IMPORTANT";
                raison = "⚠️ Formation obligatoire non encore suivie";
            }

            if (priorite == null) {
                String specialite = animatrice.getSpecialite() != null ?
                        animatrice.getSpecialite().toLowerCase() : "";
                String typeFormation = formation.getType().name().toLowerCase();
                boolean lieASpecialite =
                        (specialite.contains("musical") && typeFormation.contains("musical")) ||
                                (specialite.contains("artistique") && typeFormation.contains("artistique")) ||
                                typeFormation.contains("secourisme") ||
                                typeFormation.contains("sante") ||
                                typeFormation.contains("securite");
                if (lieASpecialite && !formationsSuivies.contains(formation.getId())) {
                    priorite = "RECOMMANDÉE";
                    raison = "⭐ Recommandée selon votre spécialité";
                }
            }

            if (priorite == null && !formationsSuivies.contains(formation.getId())) {
                priorite = "SUGGÉRÉE";
                raison = "📚 Formation disponible non encore suivie";
            }

            if (priorite != null) {
                Map<String, Object> suggestion = new HashMap<>();
                suggestion.put("formation", Map.of(
                        "id", formation.getId(),
                        "titre", formation.getTitre(),
                        "type", formation.getType(),
                        "dateFormation", formation.getDateFormation() != null ?
                                formation.getDateFormation().toString() : "",
                        "placesDisponibles", formation.getPlacesDisponibles(),
                        "obligatoire", Boolean.TRUE.equals(formation.getObligatoire()),
                        "lieu", formation.getLieu() != null ? formation.getLieu() : ""
                ));
                suggestion.put("priorite", priorite);
                suggestion.put("raison", raison);
                suggestions.add(suggestion);
            }
        }

        Map<String, Integer> ordre = Map.of(
                "URGENT", 0, "IMPORTANT", 1, "RECOMMANDÉE", 2, "SUGGÉRÉE", 3);
        suggestions.sort((a, b) ->
                ordre.getOrDefault(a.get("priorite").toString(), 4)
                        .compareTo(ordre.getOrDefault(b.get("priorite").toString(), 4)));
        return suggestions;
    }

    @Override
    public Map<String, Object> getAlertesGlobales() {
        animatriceFormationRepository.findAll().forEach(af -> {
            af.calculerStatutValidite();
            animatriceFormationRepository.save(af);
        });

        List<AnimatriceFormation> bientotExpirees =
                animatriceFormationRepository.findByStatutValidite(StatutValidite.BIENTOT_EXPIREE);
        List<AnimatriceFormation> expirees =
                animatriceFormationRepository.findByStatutValidite(StatutValidite.EXPIREE);
        List<Formation> sousInscrites = formationRepository.findFormationsSousInscrites();
        List<Formation> bientotDebutees = formationRepository.findFormationsBientotDebutees(
                LocalDate.now(), LocalDate.now().plusDays(7));

        List<Long> animatricesAvecFormationRecente =
                animatriceFormationRepository.findAnimatriceIdsAvecFormationRecente(
                        LocalDate.now().minusMonths(6));
        List<Animatrice> sansFormationRecente = animatriceRepository
                .findByStatut(StatutAnimatrice.ACTIVE).stream()
                .filter(a -> !animatricesAvecFormationRecente.contains(a.getId()))
                .collect(Collectors.toList());

        Map<String, Object> alertes = new HashMap<>();
        alertes.put("bientotExpirees", bientotExpirees);
        alertes.put("expirees", expirees);
        alertes.put("sousInscrites", sousInscrites);
        alertes.put("bientotDebutees", bientotDebutees);
        alertes.put("sansFormationRecente", sansFormationRecente);
        alertes.put("totalAlertes", bientotExpirees.size() + expirees.size() +
                sousInscrites.size() + sansFormationRecente.size());
        return alertes;
    }

    @Override
    public Map<String, Object> getAlertesAnimatrice(Long animatriceId) {
        List<AnimatriceFormation> bientotExpirees =
                animatriceFormationRepository.findByAnimatriceIdAndStatutValidite(
                        animatriceId, StatutValidite.BIENTOT_EXPIREE);
        List<AnimatriceFormation> expirees =
                animatriceFormationRepository.findFormationsExpireesByAnimatrice(animatriceId);

        List<Long> suivies = animatriceFormationRepository
                .findFormationIdsSuiviesByAnimatriceId(animatriceId);
        List<Long> inscrites = animatriceFormationRepository.findByAnimatriceId(animatriceId).stream()
                .filter(af -> StatutInscription.INSCRITE.equals(af.getStatut())
                        || StatutInscription.LISTE_ATTENTE.equals(af.getStatut()))
                .map(af -> af.getFormation().getId())
                .collect(Collectors.toList());

        List<Formation> obligatoiresNonSuivies = formationRepository.findByObligatoire(true).stream()
                .filter(f -> !suivies.contains(f.getId()) && !inscrites.contains(f.getId()))
                .collect(Collectors.toList());

        Map<String, Object> alertes = new HashMap<>();
        alertes.put("bientotExpirees", bientotExpirees);
        alertes.put("expirees", expirees);
        alertes.put("obligatoiresNonSuivies", obligatoiresNonSuivies);
        alertes.put("totalAlertes", bientotExpirees.size() + expirees.size() +
                obligatoiresNonSuivies.size());
        return alertes;
    }

    @Override
    public Map<String, Object> getStatsFormations() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", formationRepository.count());
        stats.put("ouvertes", formationRepository.countByStatut(StatutFormation.OUVERTE));
        stats.put("enCours", formationRepository.countByStatut(StatutFormation.EN_COURS));
        stats.put("terminees", formationRepository.countByStatut(StatutFormation.TERMINEE));
        stats.put("annulees", formationRepository.countByStatut(StatutFormation.ANNULEE));

        long certificats = animatriceFormationRepository.findAll().stream()
                .filter(af -> Boolean.TRUE.equals(af.getCertificationGeneree())).count();
        stats.put("totalInscriptions", animatriceFormationRepository.count());
        stats.put("certificatsGeneres", certificats);
        return stats;
    }

    // ===== HELPERS PRIVÉS =====

    private void notifierNouvelleFormation(Formation formation) {
        animatriceRepository.findByStatut(StatutAnimatrice.ACTIVE).forEach(animatrice ->
                notificationService.creerNotification(
                        "📚 Nouvelle formation disponible : '" + formation.getTitre() + "'" +
                                (formation.getPlacesMax() != null ?
                                        " — " + formation.getPlacesMax() + " places" : ""),
                        "NOUVELLE_FORMATION"
                )
        );
    }

    private void inscrireToutes(Formation formation) {
        animatriceRepository.findByStatut(StatutAnimatrice.ACTIVE).forEach(animatrice -> {
            if (!animatriceFormationRepository.existsByAnimatriceIdAndFormationId(
                    animatrice.getId(), formation.getId())) {
                AnimatriceFormation inscription = AnimatriceFormation.builder()
                        .animatrice(animatrice)
                        .formation(formation)
                        .dateInscription(LocalDate.now())
                        .statut(StatutInscription.INSCRITE)
                        .certificationGeneree(false)
                        .build();
                animatriceFormationRepository.save(inscription);
            }
        });
        notificationService.creerNotification(
                "📌 Inscription automatique à la formation obligatoire : " + formation.getTitre(),
                "FORMATION_OBLIGATOIRE"
        );
    }

    private void promouvoirListeAttente(Long formationId) {
        Formation formation = getFormationById(formationId);
        if (formation.isComplet()) return;

        List<AnimatriceFormation> listeAttente =
                animatriceFormationRepository.findByFormationIdAndStatutOrderByDateInscriptionAsc(
                        formationId, StatutInscription.LISTE_ATTENTE);

        if (!listeAttente.isEmpty()) {
            AnimatriceFormation premier = listeAttente.get(0);
            premier.setStatut(StatutInscription.INSCRITE);
            animatriceFormationRepository.save(premier);
            notificationService.creerNotification(
                    "🎉 Une place s'est libérée ! Votre inscription à '" +
                            formation.getTitre() + "' est maintenant confirmée.",
                    "PLACE_LIBEREE"
            );
        }
    }
}
