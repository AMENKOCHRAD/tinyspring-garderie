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
}

