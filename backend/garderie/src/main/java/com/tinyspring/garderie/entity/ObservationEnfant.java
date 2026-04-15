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
    @Column(nullable = false)
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
}

