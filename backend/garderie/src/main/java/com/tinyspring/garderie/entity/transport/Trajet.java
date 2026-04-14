package com.tinyspring.garderie.entity.transport;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "trajets")
public class Trajet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String pointDepart;

    @Column(nullable = false)
    private String destination;

    @Column(name = "zone_desservie")
    private String zoneDesservie;

    @Column(name = "latitude_destination")
    private Double latitudeDestination;

    @Column(name = "longitude_destination")
    private Double longitudeDestination;

    @Column(nullable = false)
    private LocalTime heureDepart;

    @Column(nullable = false)
    private LocalDate dateTrajet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_id", nullable = false)
    private Transport transport;

    public Trajet() {
    }

    public Trajet(String pointDepart, String destination, LocalDate dateTrajet, LocalTime heureDepart, Transport transport) {
        this.pointDepart = pointDepart;
        this.destination = destination;
        this.dateTrajet = dateTrajet;
        this.heureDepart = heureDepart;
        this.transport = transport;
    }

    public Long getId() {
        return id;
    }

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

    public Transport getTransport() {
        return transport;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }
}
