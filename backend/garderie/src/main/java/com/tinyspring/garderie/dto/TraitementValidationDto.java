package com.tinyspring.garderie.dto;

import java.time.LocalDate;
import java.util.List;

public class TraitementValidationDto {

    private Long traitementId;
    private String nomTraitement;
    private String description;
    private String ordonnance;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private List<String> heuresPrises;
    private String statut;

    private Long conditionId;
    private String nomCondition;
    private String typeCondition;
    private String descriptionCondition;

    private Long enfantId;
    private String nomEnfant;
    private String prenomEnfant;

    private String nomParent;
    private String emailParent;

    public TraitementValidationDto() {
    }

    public Long getTraitementId() {
        return traitementId;
    }

    public void setTraitementId(Long traitementId) {
        this.traitementId = traitementId;
    }

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

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public Long getConditionId() {
        return conditionId;
    }

    public void setConditionId(Long conditionId) {
        this.conditionId = conditionId;
    }

    public String getNomCondition() {
        return nomCondition;
    }

    public void setNomCondition(String nomCondition) {
        this.nomCondition = nomCondition;
    }

    public String getTypeCondition() {
        return typeCondition;
    }

    public void setTypeCondition(String typeCondition) {
        this.typeCondition = typeCondition;
    }

    public String getDescriptionCondition() {
        return descriptionCondition;
    }

    public void setDescriptionCondition(String descriptionCondition) {
        this.descriptionCondition = descriptionCondition;
    }

    public Long getEnfantId() {
        return enfantId;
    }

    public void setEnfantId(Long enfantId) {
        this.enfantId = enfantId;
    }

    public String getNomEnfant() {
        return nomEnfant;
    }

    public void setNomEnfant(String nomEnfant) {
        this.nomEnfant = nomEnfant;
    }

    public String getPrenomEnfant() {
        return prenomEnfant;
    }

    public void setPrenomEnfant(String prenomEnfant) {
        this.prenomEnfant = prenomEnfant;
    }

    public String getNomParent() {
        return nomParent;
    }

    public void setNomParent(String nomParent) {
        this.nomParent = nomParent;
    }

    public String getEmailParent() {
        return emailParent;
    }

    public void setEmailParent(String emailParent) {
        this.emailParent = emailParent;
    }
}