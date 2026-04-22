package com.tinyspring.garderie.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "observations_enfant")
public class ObservationEnfant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "enfant_id", nullable = false)
    private Enfant enfant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ObservationType type;

    @Column(nullable = false, length = 120)
    private String titre;

    @Column(nullable = false, length = 1000)
    private String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cree_par_user_id", nullable = false)
    private User creePar;

    @Column(nullable = false)
    private LocalDateTime creeLe = LocalDateTime.now();

    @Column(nullable = false)
    private boolean luParent = false;

    private LocalDateTime luLe;

    private LocalDateTime observeLe;

    @Enumerated(EnumType.STRING)
    private NiveauUrgence urgence;

    private Double temperature;

    @Column(length = 120)
    private String lieu;

    @Column(length = 500)
    private String symptomes;

    @Column(length = 500)
    private String actionsEffectuees;

    public Long getId() {
        return id;
    }

    public Enfant getEnfant() {
        return enfant;
    }

    public void setEnfant(Enfant enfant) {
        this.enfant = enfant;
    }

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

    public User getCreePar() {
        return creePar;
    }

    public void setCreePar(User creePar) {
        this.creePar = creePar;
    }

    public LocalDateTime getCreeLe() {
        return creeLe;
    }

    public void setCreeLe(LocalDateTime creeLe) {
        this.creeLe = creeLe;
    }

    public boolean isLuParent() {
        return luParent;
    }

    public void setLuParent(boolean luParent) {
        this.luParent = luParent;
    }

    public LocalDateTime getLuLe() {
        return luLe;
    }

    public void setLuLe(LocalDateTime luLe) {
        this.luLe = luLe;
    }

    public LocalDateTime getObserveLe() {
        return observeLe;
    }

    public void setObserveLe(LocalDateTime observeLe) {
        this.observeLe = observeLe;
    }

    public NiveauUrgence getUrgence() {
        return urgence;
    }

    public void setUrgence(NiveauUrgence urgence) {
        this.urgence = urgence;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public String getSymptomes() {
        return symptomes;
    }

    public void setSymptomes(String symptomes) {
        this.symptomes = symptomes;
    }

    public String getActionsEffectuees() {
        return actionsEffectuees;
    }

    public void setActionsEffectuees(String actionsEffectuees) {
        this.actionsEffectuees = actionsEffectuees;
    }
}
