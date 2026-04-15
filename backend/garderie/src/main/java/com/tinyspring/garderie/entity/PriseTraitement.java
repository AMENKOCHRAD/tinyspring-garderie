package com.tinyspring.garderie.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "prises_traitement",
        uniqueConstraints = @UniqueConstraint(columnNames = {"traitement_id", "date_prise", "heure_prevue"}))
public class PriseTraitement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "traitement_id", nullable = false)
    private Traitement traitement;

    @Column(name = "date_prise", nullable = false)
    private LocalDate datePrise;

    @Column(name = "heure_prevue", nullable = false, length = 10)
    private String heurePrevue;

    @ManyToOne(optional = false)
    @JoinColumn(name = "donne_par_user_id", nullable = false)
    private User donnePar;

    @Column(nullable = false)
    private LocalDateTime donneLe = LocalDateTime.now();

    @Column(length = 500)
    private String note;

    public Long getId() {
        return id;
    }

    public Traitement getTraitement() {
        return traitement;
    }

    public void setTraitement(Traitement traitement) {
        this.traitement = traitement;
    }

    public LocalDate getDatePrise() {
        return datePrise;
    }

    public void setDatePrise(LocalDate datePrise) {
        this.datePrise = datePrise;
    }

    public String getHeurePrevue() {
        return heurePrevue;
    }

    public void setHeurePrevue(String heurePrevue) {
        this.heurePrevue = heurePrevue;
    }

    public User getDonnePar() {
        return donnePar;
    }

    public void setDonnePar(User donnePar) {
        this.donnePar = donnePar;
    }

    public LocalDateTime getDonneLe() {
        return donneLe;
    }

    public void setDonneLe(LocalDateTime donneLe) {
        this.donneLe = donneLe;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

