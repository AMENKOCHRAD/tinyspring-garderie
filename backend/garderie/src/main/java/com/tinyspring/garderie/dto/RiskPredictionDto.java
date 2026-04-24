package com.tinyspring.garderie.dto;

import java.util.ArrayList;
import java.util.List;

public class RiskPredictionDto {
    private Long enfantId;
    private String enfantNom;
    private String enfantPrenom;

    private RiskLevel niveau;
    private double confiance; // 0..1
    private int fenetreJours;
    private int observationsUtilisees;
    private String derniereObservationLe; // ISO string (local)

    private List<String> facteurs = new ArrayList<>();
    private String recommandation;

    public Long getEnfantId() {
        return enfantId;
    }

    public void setEnfantId(Long enfantId) {
        this.enfantId = enfantId;
    }

    public String getEnfantNom() {
        return enfantNom;
    }

    public void setEnfantNom(String enfantNom) {
        this.enfantNom = enfantNom;
    }

    public String getEnfantPrenom() {
        return enfantPrenom;
    }

    public void setEnfantPrenom(String enfantPrenom) {
        this.enfantPrenom = enfantPrenom;
    }

    public RiskLevel getNiveau() {
        return niveau;
    }

    public void setNiveau(RiskLevel niveau) {
        this.niveau = niveau;
    }

    public double getConfiance() {
        return confiance;
    }

    public void setConfiance(double confiance) {
        this.confiance = confiance;
    }

    public int getFenetreJours() {
        return fenetreJours;
    }

    public void setFenetreJours(int fenetreJours) {
        this.fenetreJours = fenetreJours;
    }

    public int getObservationsUtilisees() {
        return observationsUtilisees;
    }

    public void setObservationsUtilisees(int observationsUtilisees) {
        this.observationsUtilisees = observationsUtilisees;
    }

    public String getDerniereObservationLe() {
        return derniereObservationLe;
    }

    public void setDerniereObservationLe(String derniereObservationLe) {
        this.derniereObservationLe = derniereObservationLe;
    }

    public List<String> getFacteurs() {
        return facteurs;
    }

    public void setFacteurs(List<String> facteurs) {
        this.facteurs = facteurs != null ? facteurs : new ArrayList<>();
    }

    public String getRecommandation() {
        return recommandation;
    }

    public void setRecommandation(String recommandation) {
        this.recommandation = recommandation;
    }
}

