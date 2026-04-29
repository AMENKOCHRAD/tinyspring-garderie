package com.tinyspring.garderie.entity.transport;

import com.tinyspring.garderie.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "demandes_transport")
public class DemandeTransport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enfant_id", nullable = false)
    private Enfant enfant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private User parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trajet_id")
    private Trajet trajet;

    @Column(nullable = false)
    private LocalDate dateDemande = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDemandeTransport statut = StatutDemandeTransport.EN_ATTENTE;

    @Column(nullable = false)
    private String pointRamassage;

    @Column(nullable = false)
    private String destinationSouhaitee;

    @Enumerated(EnumType.STRING)
    @Column(name = "sens_trajet", nullable = false)
    private SensTrajetDemandeTransport sensTrajet = SensTrajetDemandeTransport.MAISON_VERS_GARDERIE;

    @Column(name = "adresse_maison", nullable = false, length = 255)
    private String adresseMaison;

    @Column(name = "latitude_maison", nullable = false)
    private Double latitudeMaison;

    @Column(name = "longitude_maison", nullable = false)
    private Double longitudeMaison;

    @Column(name = "date_souhaitee", nullable = false)
    private LocalDate dateSouhaitee;

    @Column(name = "heure_souhaitee", nullable = false)
    private LocalTime heureSouhaitee;

    @Column(name = "suspicious", nullable = false)
    private Boolean suspicious = Boolean.FALSE;

    @Column(name = "ai_analysis_available", nullable = false)
    private Boolean aiAnalysisAvailable = Boolean.FALSE;

    @Column(name = "duplicate_detected", nullable = false)
    private Boolean duplicateDetected = Boolean.FALSE;

    @Column(name = "anomaly_score")
    private Double anomalyScore;

    @Column(name = "anomaly_level", length = 30)
    private String anomalyLevel;

    @Column(name = "anomaly_reasons", length = 2000)
    private String anomalyReasons;

    @Column(name = "ai_model_version", length = 50)
    private String aiModelVersion;

    @Column(name = "ai_analysis_error", length = 500)
    private String aiAnalysisError;

    @Column(name = "revision_request_message", length = 1000)
    private String revisionRequestMessage;

    @Column(name = "revision_requested_at")
    private LocalDateTime revisionRequestedAt;

    public DemandeTransport() {
    }

    public DemandeTransport(Enfant enfant,
                            User parent,
                            Trajet trajet,
                            StatutDemandeTransport statut,
                            String pointRamassage,
                            String destinationSouhaitee,
                            SensTrajetDemandeTransport sensTrajet,
                            String adresseMaison,
                            Double latitudeMaison,
                            Double longitudeMaison,
                            LocalDate dateSouhaitee,
                            LocalTime heureSouhaitee) {
        this.enfant = enfant;
        this.parent = parent;
        this.trajet = trajet;
        this.statut = statut;
        this.pointRamassage = pointRamassage;
        this.destinationSouhaitee = destinationSouhaitee;
        this.sensTrajet = sensTrajet;
        this.adresseMaison = adresseMaison;
        this.latitudeMaison = latitudeMaison;
        this.longitudeMaison = longitudeMaison;
        this.dateSouhaitee = dateSouhaitee;
        this.heureSouhaitee = heureSouhaitee;
        this.dateDemande = LocalDate.now();
    }

    public Long getId() {
        return id;
    }

    public Enfant getEnfant() {
        return enfant;
    }

    public void setEnfant(Enfant enfant) {
        this.enfant = enfant;
    }

    public User getParent() {
        return parent;
    }

    public void setParent(User parent) {
        this.parent = parent;
    }

    public Trajet getTrajet() {
        return trajet;
    }

    public void setTrajet(Trajet trajet) {
        this.trajet = trajet;
    }

    public LocalDate getDateDemande() {
        return dateDemande;
    }

    public void setDateDemande(LocalDate dateDemande) {
        this.dateDemande = dateDemande;
    }

    public StatutDemandeTransport getStatut() {
        return statut;
    }

    public void setStatut(StatutDemandeTransport statut) {
        this.statut = statut;
    }

    public String getPointRamassage() {
        return pointRamassage;
    }

    public void setPointRamassage(String pointRamassage) {
        this.pointRamassage = pointRamassage;
    }

    public String getDestinationSouhaitee() {
        return destinationSouhaitee;
    }

    public void setDestinationSouhaitee(String destinationSouhaitee) {
        this.destinationSouhaitee = destinationSouhaitee;
    }

    public SensTrajetDemandeTransport getSensTrajet() {
        return sensTrajet;
    }

    public void setSensTrajet(SensTrajetDemandeTransport sensTrajet) {
        this.sensTrajet = sensTrajet;
    }

    public String getAdresseMaison() {
        return adresseMaison;
    }

    public void setAdresseMaison(String adresseMaison) {
        this.adresseMaison = adresseMaison;
    }

    public Double getLatitudeMaison() {
        return latitudeMaison;
    }

    public void setLatitudeMaison(Double latitudeMaison) {
        this.latitudeMaison = latitudeMaison;
    }

    public Double getLongitudeMaison() {
        return longitudeMaison;
    }

    public void setLongitudeMaison(Double longitudeMaison) {
        this.longitudeMaison = longitudeMaison;
    }

    public LocalDate getDateSouhaitee() {
        return dateSouhaitee;
    }

    public void setDateSouhaitee(LocalDate dateSouhaitee) {
        this.dateSouhaitee = dateSouhaitee;
    }

    public LocalTime getHeureSouhaitee() {
        return heureSouhaitee;
    }

    public void setHeureSouhaitee(LocalTime heureSouhaitee) {
        this.heureSouhaitee = heureSouhaitee;
    }

    public Boolean getSuspicious() {
        return suspicious;
    }

    public void setSuspicious(Boolean suspicious) {
        this.suspicious = suspicious;
    }

    public Boolean getAiAnalysisAvailable() {
        return aiAnalysisAvailable;
    }

    public void setAiAnalysisAvailable(Boolean aiAnalysisAvailable) {
        this.aiAnalysisAvailable = aiAnalysisAvailable;
    }

    public Boolean getDuplicateDetected() {
        return duplicateDetected;
    }

    public void setDuplicateDetected(Boolean duplicateDetected) {
        this.duplicateDetected = duplicateDetected;
    }

    public Double getAnomalyScore() {
        return anomalyScore;
    }

    public void setAnomalyScore(Double anomalyScore) {
        this.anomalyScore = anomalyScore;
    }

    public String getAnomalyLevel() {
        return anomalyLevel;
    }

    public void setAnomalyLevel(String anomalyLevel) {
        this.anomalyLevel = anomalyLevel;
    }

    public String getAnomalyReasons() {
        return anomalyReasons;
    }

    public void setAnomalyReasons(String anomalyReasons) {
        this.anomalyReasons = anomalyReasons;
    }

    public String getAiModelVersion() {
        return aiModelVersion;
    }

    public void setAiModelVersion(String aiModelVersion) {
        this.aiModelVersion = aiModelVersion;
    }

    public String getAiAnalysisError() {
        return aiAnalysisError;
    }

    public void setAiAnalysisError(String aiAnalysisError) {
        this.aiAnalysisError = aiAnalysisError;
    }

    public String getRevisionRequestMessage() {
        return revisionRequestMessage;
    }

    public void setRevisionRequestMessage(String revisionRequestMessage) {
        this.revisionRequestMessage = revisionRequestMessage;
    }

    public LocalDateTime getRevisionRequestedAt() {
        return revisionRequestedAt;
    }

    public void setRevisionRequestedAt(LocalDateTime revisionRequestedAt) {
        this.revisionRequestedAt = revisionRequestedAt;
    }
}
