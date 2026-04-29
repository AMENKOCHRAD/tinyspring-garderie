package com.tinyspring.garderie.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "traitements")
public class Traitement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomTraitement;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private String ordonnance;

    @Column(nullable = false)
    private LocalDate dateDebut;

    private LocalDate dateFin;

    @ElementCollection
    @CollectionTable(name = "traitement_heures", joinColumns = @JoinColumn(name = "traitement_id"))
    @Column(name = "heure")
    private List<String> heuresPrises; // liste d'heures pour prises quotidiennes

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StatutTraitement statut = StatutTraitement.EN_ATTENTE_VALIDATION;

    @Column(length = 500)
    private String autoValidationNote;

    // Relation avec ConditionSanitaire
    @ManyToOne
    @JoinColumn(name = "condition_id", nullable = false)
    @JsonIgnore
    private ConditionSanitaire conditionSanitaire;

    public Traitement() {}

    // ===================== Getters et Setters =====================
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomTraitement() {
        return nomTraitement;
    }

    public void setNomTraitement(String nomTraitement) {
        this.nomTraitement = nomTraitement;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOrdonnance() {
        return ordonnance;
    }

    public void setOrdonnance(String ordonnance) {
        this.ordonnance = ordonnance;
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

    public List<String> getHeuresPrises() {
        return heuresPrises;
    }

    public void setHeuresPrises(List<String> heuresPrises) {
        this.heuresPrises = heuresPrises;
    }

    public StatutTraitement getStatut() {
        return statut;
    }

    public void setStatut(StatutTraitement statut) {
        this.statut = statut;
    }

    public String getAutoValidationNote() {
        return autoValidationNote;
    }

    public void setAutoValidationNote(String autoValidationNote) {
        this.autoValidationNote = autoValidationNote;
    }

    public ConditionSanitaire getConditionSanitaire() {
        return conditionSanitaire;
    }

    public void setConditionSanitaire(ConditionSanitaire conditionSanitaire) {
        this.conditionSanitaire = conditionSanitaire;
    }
}
