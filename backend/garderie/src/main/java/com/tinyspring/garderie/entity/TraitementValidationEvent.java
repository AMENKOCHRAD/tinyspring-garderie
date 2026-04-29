package com.tinyspring.garderie.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "traitement_validation_events")
public class TraitementValidationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "traitement_id", nullable = false)
    private Traitement traitement;

    @Column(nullable = false, length = 32)
    private String decision; // ACCEPTE / REFUSE / A_VERIFIER / VALIDE_ADMIN / REFUSE_ADMIN ...

    @Column(nullable = false, length = 32)
    private String source; // RULES / ML / ADMIN

    private Double confiance;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String facteursJson;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private LocalDateTime creeLe = LocalDateTime.now();

    @Column(length = 160)
    private String creeParEmail; // admin email, or "SYSTEM"

    public Long getId() {
        return id;
    }

    public Traitement getTraitement() {
        return traitement;
    }

    public void setTraitement(Traitement traitement) {
        this.traitement = traitement;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Double getConfiance() {
        return confiance;
    }

    public void setConfiance(Double confiance) {
        this.confiance = confiance;
    }

    public String getFacteursJson() {
        return facteursJson;
    }

    public void setFacteursJson(String facteursJson) {
        this.facteursJson = facteursJson;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreeLe() {
        return creeLe;
    }

    public void setCreeLe(LocalDateTime creeLe) {
        this.creeLe = creeLe;
    }

    public String getCreeParEmail() {
        return creeParEmail;
    }

    public void setCreeParEmail(String creeParEmail) {
        this.creeParEmail = creeParEmail;
    }
}

