package com.tinyspring.garderie.dto;

import java.time.LocalDate;
import java.util.List;

public class TraitementUpdateDto {
    private String nomTraitement;
    private String description;
    private String ordonnance;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private List<String> heuresPrises;

    public String getNomTraitement() {
        return nomTraitement;
    }

    public void setNomTraitement(String nomTraitement) {
        this.nomTraitement = nomTraitement;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOrdonnance() {
        return ordonnance;
    }

    public void setOrdonnance(String ordonnance) {
        this.ordonnance = ordonnance;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public List<String> getHeuresPrises() {
        return heuresPrises;
    }

    public void setHeuresPrises(List<String> heuresPrises) {
        this.heuresPrises = heuresPrises;
    }
}

