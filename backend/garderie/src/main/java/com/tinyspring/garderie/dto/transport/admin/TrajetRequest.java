package com.tinyspring.garderie.dto.transport.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public class TrajetRequest {

    @NotBlank(message = "Le point de depart est obligatoire")
    @Size(min = 3, max = 80, message = "Le point de depart doit contenir entre 3 et 80 caracteres")
    @Pattern(regexp = "^(?! ).*(?<! )$", message = "Le point de depart ne doit pas commencer ou finir par un espace")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ0-9\\s\\-()',]+$", message = "Le point de depart contient des caracteres non autorises")
    private String pointDepart;

    @NotBlank(message = "La destination est obligatoire")
    @Size(min = 3, max = 80, message = "La destination doit contenir entre 3 et 80 caracteres")
    @Pattern(regexp = "^(?! ).*(?<! )$", message = "La destination ne doit pas commencer ou finir par un espace")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ0-9\\s\\-()',]+$", message = "La destination contient des caracteres non autorises")
    private String destination;

    @NotNull(message = "L'heure de depart est obligatoire")
    private LocalTime heureDepart;

    @NotNull(message = "La date du trajet est obligatoire")
    private LocalDate dateTrajet;

    @NotNull(message = "Le transport est obligatoire")
    private Long transportId;

    @Size(max = 120, message = "La zone desservie ne doit pas depasser 120 caracteres")
    private String zoneDesservie;

    private Double latitudeDestination;

    private Double longitudeDestination;

    public String getPointDepart() {
        return pointDepart;
    }

    public void setPointDepart(String pointDepart) {
        this.pointDepart = pointDepart;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public LocalTime getHeureDepart() {
        return heureDepart;
    }

    public void setHeureDepart(LocalTime heureDepart) {
        this.heureDepart = heureDepart;
    }

    public LocalDate getDateTrajet() {
        return dateTrajet;
    }

    public void setDateTrajet(LocalDate dateTrajet) {
        this.dateTrajet = dateTrajet;
    }

    public Long getTransportId() {
        return transportId;
    }

    public void setTransportId(Long transportId) {
        this.transportId = transportId;
    }

    public String getZoneDesservie() {
        return zoneDesservie;
    }

    public void setZoneDesservie(String zoneDesservie) {
        this.zoneDesservie = zoneDesservie;
    }

    public Double getLatitudeDestination() {
        return latitudeDestination;
    }

    public void setLatitudeDestination(Double latitudeDestination) {
        this.latitudeDestination = latitudeDestination;
    }

    public Double getLongitudeDestination() {
        return longitudeDestination;
    }

    public void setLongitudeDestination(Double longitudeDestination) {
        this.longitudeDestination = longitudeDestination;
    }
}
