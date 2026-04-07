package com.tinyspring.garderie.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "conditions_sanitaires")
public class ConditionSanitaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomCondition;  // ex: "Asthme"

    @Column(nullable = false)
    private String type;  // "Chronique" ou "Temporaire"

    private String description;

    private LocalDate dateDebut;

    private LocalDate dateFin; // pour les conditions temporaires

    // Relation avec Enfant
    @ManyToOne
    @JoinColumn(name = "enfant_id", nullable = false)
    private Enfant enfant;

    public ConditionSanitaire() {}

    public ConditionSanitaire(String nomCondition, String type, String description, LocalDate dateDebut,
                              LocalDate dateFin, Enfant enfant) {
        this.nomCondition = nomCondition;
        this.type = type;
        this.description = description;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.enfant = enfant;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public String getNomCondition() { return nomCondition; }
    public void setNomCondition(String nomCondition) { this.nomCondition = nomCondition; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }
    public Enfant getEnfant() { return enfant; }
    public void setEnfant(Enfant enfant) { this.enfant = enfant; }
}