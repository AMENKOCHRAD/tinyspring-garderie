package com.tinyspring.garderie.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "conditions_sanitaires")
public class ConditionSanitaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomCondition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeConditionSanitaire type;

    private String description;

    private LocalDate dateDebut;

    private LocalDate dateFin;

    @ManyToOne
    @JoinColumn(name = "enfant_id", nullable = false)
    @JsonIgnore
    private Enfant enfant;

    public ConditionSanitaire() {}

    public Long getId() {
        return id;
    }

    public String getNomCondition() {
        return nomCondition;
    }

    public void setNomCondition(String nomCondition) {
        this.nomCondition = nomCondition;
    }

    public TypeConditionSanitaire getType() {
        return type;
    }

    public void setType(TypeConditionSanitaire type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Enfant getEnfant() {
        return enfant;
    }

    public void setEnfant(Enfant enfant) {
        this.enfant = enfant;
    }
}