package com.tinyspring.garderie.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "demandes_transport")
public class DemandeTransport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enfant_id", nullable = false)
    private Enfant enfant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private User parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trajet_id", nullable = false)
    private Trajet trajet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDemandeTransport statut = StatutDemandeTransport.EN_ATTENTE;

    @Column(nullable = false)
    private String pointRamassage;

    public DemandeTransport() {
    }

    public DemandeTransport(Enfant enfant, User parent, Trajet trajet, StatutDemandeTransport statut, String pointRamassage) {
        this.enfant = enfant;
        this.parent = parent;
        this.trajet = trajet;
        this.statut = statut;
        this.pointRamassage = pointRamassage;
    }

    public Long getId() {
        return id;
    }

    public Enfant getEnfant() {
        return enfant;
    }

    public void setEnfant(Enfant enfant) {
        this.enfant = enfant;
    }

    public User getParent() {
        return parent;
    }

    public void setParent(User parent) {
        this.parent = parent;
    }

    public Trajet getTrajet() {
        return trajet;
    }

    public void setTrajet(Trajet trajet) {
        this.trajet = trajet;
    }

    public StatutDemandeTransport getStatut() {
        return statut;
    }

    public void setStatut(StatutDemandeTransport statut) {
        this.statut = statut;
    }

    public String getPointRamassage() {
        return pointRamassage;
    }

    public void setPointRamassage(String pointRamassage) {
        this.pointRamassage = pointRamassage;
    }
}
