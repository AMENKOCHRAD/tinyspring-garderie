package com.tinyspring.garderie.dto;

public class ObservationDto {
    private Long id;
    private Long enfantId;
    private String enfantNom;
    private String enfantPrenom;
    private String type;
    private String titre;
    private String description;
    private String creeLe;
    private String creeParNom;
    private boolean luParent;
    private String luLe;
    private String observeLe;
    private String urgence;
    private Double temperature;
    private String lieu;
    private String symptomes;
    private String actionsEffectuees;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
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

    public String getCreeLe() {
        return creeLe;
    }

    public void setCreeLe(String creeLe) {
        this.creeLe = creeLe;
    }

    public String getCreeParNom() {
        return creeParNom;
    }

    public void setCreeParNom(String creeParNom) {
        this.creeParNom = creeParNom;
    }

    public boolean isLuParent() {
        return luParent;
    }

    public void setLuParent(boolean luParent) {
        this.luParent = luParent;
    }

    public String getLuLe() {
        return luLe;
    }

    public void setLuLe(String luLe) {
        this.luLe = luLe;
    }

    public String getObserveLe() {
        return observeLe;
    }

    public void setObserveLe(String observeLe) {
        this.observeLe = observeLe;
    }

    public String getUrgence() {
        return urgence;
    }

    public void setUrgence(String urgence) {
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
}
