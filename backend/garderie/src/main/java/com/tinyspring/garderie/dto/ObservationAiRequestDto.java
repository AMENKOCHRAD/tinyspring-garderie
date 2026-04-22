package com.tinyspring.garderie.dto;

import com.tinyspring.garderie.entity.NiveauUrgence;
import com.tinyspring.garderie.entity.ObservationType;

public class ObservationAiRequestDto {
    private Long enfantId;
    private ObservationType type;
    private String titre;
    private NiveauUrgence urgence;
    private Double temperature;
    private String lieu;
    private String symptomes;
    private String actionsEffectuees;
    private String contexte;

    public Long getEnfantId() {
        return enfantId;
    }

    public void setEnfantId(Long enfantId) {
        this.enfantId = enfantId;
    }

    public ObservationType getType() {
        return type;
    }

    public void setType(ObservationType type) {
        this.type = type;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public NiveauUrgence getUrgence() {
        return urgence;
    }

    public void setUrgence(NiveauUrgence urgence) {
        this.urgence = urgence;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public String getSymptomes() {
        return symptomes;
    }

    public void setSymptomes(String symptomes) {
        this.symptomes = symptomes;
    }

    public String getActionsEffectuees() {
        return actionsEffectuees;
    }

    public void setActionsEffectuees(String actionsEffectuees) {
        this.actionsEffectuees = actionsEffectuees;
    }

    public String getContexte() {
        return contexte;
    }

    public void setContexte(String contexte) {
        this.contexte = contexte;
    }
}

