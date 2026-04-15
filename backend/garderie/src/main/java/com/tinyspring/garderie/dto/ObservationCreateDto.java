package com.tinyspring.garderie.dto;

import com.tinyspring.garderie.entity.ObservationType;

public class ObservationCreateDto {
    private ObservationType type;
    private String titre;
    private String description;

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
}

