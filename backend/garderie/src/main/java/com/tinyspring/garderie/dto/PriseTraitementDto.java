package com.tinyspring.garderie.dto;

public class PriseTraitementDto {
    private Long id;
    private Long traitementId;
    private String nomTraitement;
    private Long enfantId;
    private String enfantNom;
    private String enfantPrenom;
    private String datePrise;
    private String heurePrevue;
    private String donneLe;
    private String donneParNom;
    private String note;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getDatePrise() {
        return datePrise;
    }

    public void setDatePrise(String datePrise) {
        this.datePrise = datePrise;
    }

    public String getHeurePrevue() {
        return heurePrevue;
    }

    public void setHeurePrevue(String heurePrevue) {
        this.heurePrevue = heurePrevue;
    }

    public String getDonneLe() {
        return donneLe;
    }

    public void setDonneLe(String donneLe) {
        this.donneLe = donneLe;
    }

    public String getDonneParNom() {
        return donneParNom;
    }

    public void setDonneParNom(String donneParNom) {
        this.donneParNom = donneParNom;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

