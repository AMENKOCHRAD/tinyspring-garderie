package com.tinyspring.garderie.dto;

public class RealtimeNotificationDto {
    private String type;
    private Long unreadObservationsCount;
    private Long enfantId;
    private String enfantNom;
    private String enfantPrenom;
    private Long observationId;
    private String titre;
    private String creeLe;

    private Long traitementId;
    private String nomTraitement;
    private String heurePrevue;
    private String datePrise;
    private String validationNote;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getUnreadObservationsCount() {
        return unreadObservationsCount;
    }

    public void setUnreadObservationsCount(Long unreadObservationsCount) {
        this.unreadObservationsCount = unreadObservationsCount;
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

    public Long getObservationId() {
        return observationId;
    }

    public void setObservationId(Long observationId) {
        this.observationId = observationId;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getCreeLe() {
        return creeLe;
    }

    public void setCreeLe(String creeLe) {
        this.creeLe = creeLe;
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

    public String getHeurePrevue() {
        return heurePrevue;
    }

    public void setHeurePrevue(String heurePrevue) {
        this.heurePrevue = heurePrevue;
    }

    public String getDatePrise() {
        return datePrise;
    }

    public void setDatePrise(String datePrise) {
        this.datePrise = datePrise;
    }

    public String getValidationNote() {
        return validationNote;
    }

    public void setValidationNote(String validationNote) {
        this.validationNote = validationNote;
    }
}
