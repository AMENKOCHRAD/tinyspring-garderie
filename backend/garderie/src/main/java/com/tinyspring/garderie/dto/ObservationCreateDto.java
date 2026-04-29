package com.tinyspring.garderie.dto;

import com.tinyspring.garderie.entity.ObservationType;
import com.tinyspring.garderie.entity.NiveauUrgence;

public class ObservationCreateDto {
    private ObservationType type;
    private String titre;
    private String description;
    private String observeLe;
    private NiveauUrgence urgence;
    private Double temperature;
    private String lieu;
    private String symptomes;
    private String actionsEffectuees;
    private Boolean forceCreate;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getObserveLe() {
        return observeLe;
    }

    public void setObserveLe(String observeLe) {
        this.observeLe = observeLe;
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

    public Boolean getForceCreate() {
        return forceCreate;
    }

    public void setForceCreate(Boolean forceCreate) {
        this.forceCreate = forceCreate;
    }
}
