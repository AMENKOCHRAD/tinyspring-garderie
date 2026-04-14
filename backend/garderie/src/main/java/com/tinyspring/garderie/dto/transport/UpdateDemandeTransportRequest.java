package com.tinyspring.garderie.dto.transport;

import com.tinyspring.garderie.entity.transport.SensTrajetDemandeTransport;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public class UpdateDemandeTransportRequest {

    @NotNull(message = "L'identifiant de l'enfant est obligatoire")
    private Long enfantId;

    @NotNull(message = "Le sens du trajet est obligatoire")
    private SensTrajetDemandeTransport sensTrajet;

    @NotBlank(message = "L'adresse de la maison est obligatoire")
    private String adresseMaison;

    @NotNull(message = "La latitude de la maison est obligatoire")
    @DecimalMin(value = "-90.0", message = "Latitude invalide")
    @DecimalMax(value = "90.0", message = "Latitude invalide")
    private Double latitudeMaison;

    @NotNull(message = "La longitude de la maison est obligatoire")
    @DecimalMin(value = "-180.0", message = "Longitude invalide")
    @DecimalMax(value = "180.0", message = "Longitude invalide")
    private Double longitudeMaison;

    private LocalDate dateSouhaitee;

    private LocalTime heureSouhaitee;

    public Long getEnfantId() {
        return enfantId;
    }

    public void setEnfantId(Long enfantId) {
        this.enfantId = enfantId;
    }

    public SensTrajetDemandeTransport getSensTrajet() {
        return sensTrajet;
    }

    public void setSensTrajet(SensTrajetDemandeTransport sensTrajet) {
        this.sensTrajet = sensTrajet;
    }

    public String getAdresseMaison() {
        return adresseMaison;
    }

    public void setAdresseMaison(String adresseMaison) {
        this.adresseMaison = adresseMaison;
    }

    public Double getLatitudeMaison() {
        return latitudeMaison;
    }

    public void setLatitudeMaison(Double latitudeMaison) {
        this.latitudeMaison = latitudeMaison;
    }

    public Double getLongitudeMaison() {
        return longitudeMaison;
    }

    public void setLongitudeMaison(Double longitudeMaison) {
        this.longitudeMaison = longitudeMaison;
    }

    public LocalDate getDateSouhaitee() {
        return dateSouhaitee;
    }

    public void setDateSouhaitee(LocalDate dateSouhaitee) {
        this.dateSouhaitee = dateSouhaitee;
    }

    public LocalTime getHeureSouhaitee() {
        return heureSouhaitee;
    }

    public void setHeureSouhaitee(LocalTime heureSouhaitee) {
        this.heureSouhaitee = heureSouhaitee;
    }
}
