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

    // ===== MAPPING SPÉCIALITÉ → TYPES DE FORMATIONS =====
    // Structure : specialite → { TypeFormation → [priorite, raison] }
    private static final Map<String, Map<String, String[]>> SPECIALITE_MAPPING = new LinkedHashMap<>();

    static {
        SPECIALITE_MAPPING.put("éveil musical", Map.of(
                "MUSICAL",    new String[]{"RECOMMANDÉE", "🎵 Directement liée à votre spécialité en éveil musical"},
                "ARTISTIQUE", new String[]{"SUGGÉRÉE",    "🎨 Complémentaire à l'éveil musical"},
                "PEDAGOGIE",  new String[]{"SUGGÉRÉE",    "📚 Enrichit votre approche pédagogique musicale"}
        ));

        SPECIALITE_MAPPING.put("arts plastiques", Map.of(
                "ARTISTIQUE", new String[]{"RECOMMANDÉE", "🎨 Directement liée à votre spécialité en arts plastiques"},
                "PEDAGOGIE",  new String[]{"SUGGÉRÉE",    "📚 Renforce vos méthodes pédagogiques créatives"}
        ));

        SPECIALITE_MAPPING.put("activités motrices", Map.of(
                "SECOURISME", new String[]{"IMPORTANT",   "🛡️ Essentielle pour encadrer les activités physiques en sécurité"},
                "SANTE",      new String[]{"RECOMMANDÉE", "❤️ Complémentaire à l'encadrement moteur des enfants"},
                "SECURITE",   new String[]{"RECOMMANDÉE", "🔒 Indispensable pour la sécurité des activités motrices"},
                "PEDAGOGIE",  new String[]{"SUGGÉRÉE",    "📚 Approches pédagogiques adaptées aux activités motrices"}
        ));

        SPECIALITE_MAPPING.put("premiers secours", Map.of(
                "SECOURISME", new String[]{"RECOMMANDÉE", "🛡️ Directement liée à votre spécialité en premiers secours"},
                "SANTE",      new String[]{"IMPORTANT",   "❤️ Approfondit vos compétences en santé et soins"},
                "SECURITE",   new String[]{"RECOMMANDÉE", "🔒 Renforce votre expertise en sécurité enfant"}
        ));

        SPECIALITE_MAPPING.put("psychomotricité", Map.of(
                "PEDAGOGIE",    new String[]{"RECOMMANDÉE", "📚 Enrichit votre approche psychomotrice avec des outils pédagogiques"},
                "COMPORTEMENT", new String[]{"RECOMMANDÉE", "🧠 Complémentaire à la psychomotricité et gestion comportementale"},
                "SANTE",        new String[]{"SUGGÉRÉE",    "❤️ Utile pour le suivi du développement moteur et sanitaire"}
        ));

        SPECIALITE_MAPPING.put("éveil sensoriel", Map.of(
                "PEDAGOGIE",  new String[]{"RECOMMANDÉE", "📚 Méthodes pédagogiques pour enrichir l'éveil sensoriel"},
                "ARTISTIQUE", new String[]{"RECOMMANDÉE", "🎨 L'art stimule les sens et complète l'éveil sensoriel"},
                "SANTE",      new String[]{"SUGGÉRÉE",    "❤️ Lien entre développement sensoriel et santé de l'enfant"}
        ));

        SPECIALITE_MAPPING.put("contes et langage", Map.of(
                "PEDAGOGIE",    new String[]{"RECOMMANDÉE", "📚 Directement liée au développement du langage et à la pédagogie"},
                "COMPORTEMENT", new String[]{"SUGGÉRÉE",    "🧠 Les contes sont un outil pour le comportement et l'expression"}
        ));

        SPECIALITE_MAPPING.put("jeux éducatifs", Map.of(
                "PEDAGOGIE",    new String[]{"RECOMMANDÉE", "📚 Directement liée à votre pratique des jeux éducatifs"},
                "COMPORTEMENT", new String[]{"RECOMMANDÉE", "🧠 Le jeu favorise le développement comportemental"},
                "ARTISTIQUE",   new String[]{"SUGGÉRÉE",    "🎨 Les jeux créatifs enrichissent votre pratique"}
        ));

        SPECIALITE_MAPPING.put("nutrition enfantine", Map.of(
                "NUTRITION",  new String[]{"RECOMMANDÉE", "🥗 Directement liée à votre spécialité en nutrition enfantine"},
                "SANTE",      new String[]{"IMPORTANT",   "❤️ La santé et la nutrition sont indissociables"},
                "SECURITE",   new String[]{"SUGGÉRÉE",    "🔒 Sécurité alimentaire et allergies alimentaires"}
        ));

        SPECIALITE_MAPPING.put("soin et hygiène", Map.of(
                "SANTE",      new String[]{"RECOMMANDÉE", "❤️ Directement liée à votre spécialité en soins et hygiène"},
                "SECOURISME", new String[]{"IMPORTANT",   "🛡️ Les premiers secours complètent les soins quotidiens"},
                "NUTRITION",  new String[]{"SUGGÉRÉE",    "🥗 Hygiène alimentaire et nutrition vont de pair"}
        ));

        SPECIALITE_MAPPING.put("activités aquatiques", Map.of(
                "SECOURISME", new String[]{"IMPORTANT",   "🛡️ Indispensable pour encadrer les activités aquatiques en sécurité"},
                "SECURITE",   new String[]{"IMPORTANT",   "🔒 La sécurité aquatique est une priorité absolue"},
                "SANTE",      new String[]{"RECOMMANDÉE", "❤️ Lien entre activités aquatiques et santé de l'enfant"}
        ));

        SPECIALITE_MAPPING.put("danse et expression corporelle", Map.of(
                "MUSICAL",    new String[]{"RECOMMANDÉE", "🎵 La musique est au cœur de la danse et l'expression"},
                "ARTISTIQUE", new String[]{"RECOMMANDÉE", "🎨 Complémentaire à votre pratique d'expression corporelle"},
                "PEDAGOGIE",  new String[]{"SUGGÉRÉE",    "📚 Approches pédagogiques pour l'enseignement de la danse"}
        ));

        SPECIALITE_MAPPING.put("théâtre et marionnettes", Map.of(
                "ARTISTIQUE", new String[]{"RECOMMANDÉE", "🎨 Directement liée à votre pratique théâtrale"},
                "COMPORTEMENT",new String[]{"RECOMMANDÉE", "🧠 Le théâtre développe l'expression émotionnelle"},
                "PEDAGOGIE",  new String[]{"SUGGÉRÉE",    "📚 Outils pédagogiques pour l'enseignement par le jeu dramatique"}
        ));

        SPECIALITE_MAPPING.put("jardinage et nature", Map.of(
                "SANTE",     new String[]{"RECOMMANDÉE", "❤️ Le contact avec la nature contribue au bien-être"},
                "NUTRITION", new String[]{"RECOMMANDÉE", "🥗 Le jardinage sensibilise à la nutrition naturelle"},
                "SECURITE",  new String[]{"SUGGÉRÉE",    "🔒 Sécurité lors des activités extérieures en nature"}
        ));

        SPECIALITE_MAPPING.put("informatique enfantine", Map.of(
                "PEDAGOGIE",  new String[]{"RECOMMANDÉE", "📚 Méthodes pédagogiques pour l'enseignement numérique"},
                "SECURITE",   new String[]{"SUGGÉRÉE",    "🔒 Sécurité numérique et usage responsable des écrans"},
                "COMPORTEMENT",new String[]{"SUGGÉRÉE",   "🧠 Impact du numérique sur le comportement des enfants"}
        ));
    }

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

    // ===== SUGGESTIONS AMÉLIORÉES PAR SPÉCIALITÉ =====

    @Override
    public List<Map<String, Object>> getSuggestions(Long animatriceId) {
        Animatrice animatrice = animatriceRepository.findById(animatriceId)
                .orElseThrow(() -> new RuntimeException("Animatrice non trouvée"));

        List<Long> formationsSuivies =
                animatriceFormationRepository.findFormationIdsSuiviesByAnimatriceId(animatriceId);
        List<AnimatriceFormation> formationsExpirees =
                animatriceFormationRepository.findFormationsExpireesByAnimatrice(animatriceId);
        List<Formation> toutesFormations = formationRepository.findByStatut(StatutFormation.OUVERTE);

        // Normaliser la spécialité de l'animatrice (minuscules, sans accents pour comparaison)
        String specialite = animatrice.getSpecialite() != null
                ? animatrice.getSpecialite().toLowerCase().trim()
                : "";

        // Récupérer le mapping de priorités pour cette spécialité
        Map<String, String[]> prioritesParType = SPECIALITE_MAPPING.getOrDefault(specialite, Map.of());

        List<Map<String, Object>> suggestions = new ArrayList<>();

        for (Formation formation : toutesFormations) {
            // Exclure les formations où l'animatrice est déjà inscrite ou en attente
            if (animatriceFormationRepository.existsByAnimatriceIdAndFormationId(
                    animatriceId, formation.getId())) continue;

            String priorite = null;
            String raison   = null;
            String typeFormation = formation.getType().name();

            // === RÈGLE 1 : Recyclage urgent (formation expirée du même type) ===
            boolean estRecyclage = formationsExpirees.stream()
                    .anyMatch(af -> af.getFormation().getType().equals(formation.getType()));
            if (estRecyclage) {
                priorite = "URGENT";
                raison   = "🔁 Recyclage requis — votre certification de type "
                        + typeFormation + " a expiré";
            }

            // === RÈGLE 2 : Formation obligatoire non suivie ===
            if (priorite == null && Boolean.TRUE.equals(formation.getObligatoire())
                    && !formationsSuivies.contains(formation.getId())) {
                priorite = "IMPORTANT";
                raison   = "⚠️ Formation obligatoire non encore suivie";
            }

            // === RÈGLE 3 : Recommandation basée sur la spécialité ===
            if (priorite == null && !prioritesParType.isEmpty()) {
                String[] mapping = prioritesParType.get(typeFormation);
                if (mapping != null) {
                    priorite = mapping[0];
                    raison   = mapping[1];
                }
            }

            // === RÈGLE 4 : Toutes les formations SECOURISME et SANTE sont toujours suggérées
            //               (sécurité enfant = priorité universelle) ===
            if (priorite == null
                    && (typeFormation.equals("SECOURISME") || typeFormation.equals("SANTE"))) {
                priorite = "SUGGÉRÉE";
                raison   = typeFormation.equals("SECOURISME")
                        ? "🛡️ Le secourisme est essentiel pour toute animatrice de garderie"
                        : "❤️ La formation en santé est recommandée pour toutes les animatrices";
            }

            // === RÈGLE 5 : Formations disponibles non suivies (suggestion générale) ===
            if (priorite == null && !formationsSuivies.contains(formation.getId())) {
                priorite = "SUGGÉRÉE";
                raison   = "📚 Formation disponible non encore suivie — élargissez vos compétences";
            }

            if (priorite != null) {
                Map<String, Object> suggestion = new HashMap<>();
                suggestion.put("formation", Map.of(
                        "id",               formation.getId(),
                        "titre",            formation.getTitre(),
                        "type",             formation.getType(),
                        "dateFormation",    formation.getDateFormation() != null
                                ? formation.getDateFormation().toString() : "",
                        "placesDisponibles", formation.getPlacesDisponibles(),
                        "obligatoire",      Boolean.TRUE.equals(formation.getObligatoire()),
                        "lieu",             formation.getLieu() != null ? formation.getLieu() : ""
                ));
                suggestion.put("priorite", priorite);
                suggestion.put("raison", raison);
                suggestions.add(suggestion);
            }
        }

        // Tri par priorité : URGENT > IMPORTANT > RECOMMANDÉE > SUGGÉRÉE
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