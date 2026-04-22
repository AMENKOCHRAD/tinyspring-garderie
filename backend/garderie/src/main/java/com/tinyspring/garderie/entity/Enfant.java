package com.tinyspring.garderie.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "enfants")
public class Enfant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false)
    private LocalDate dateNaissance;

    private String groupeSanguin;

    private String allergies;

    private String contactUrgence;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String photo;

    @Column(nullable = false)
    private boolean archive = false;

    @ManyToOne
    @JoinColumn(name = "parent_id", nullable = false)
    private User parent;

    public Enfant() {}

    public Enfant(String nom, String prenom, LocalDate dateNaissance, String groupeSanguin,
                  String allergies, String contactUrgence, String photo, User parent) {
        this.nom = nom;
        this.prenom = prenom;
        this.dateNaissance = dateNaissance;
        this.groupeSanguin = groupeSanguin;
        this.allergies = allergies;
        this.contactUrgence = contactUrgence;
        this.photo = photo;
        this.parent = parent;
    }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }
    public String getGroupeSanguin() { return groupeSanguin; }
    public void setGroupeSanguin(String groupeSanguin) { this.groupeSanguin = groupeSanguin; }
    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }
    public String getContactUrgence() { return contactUrgence; }
    public void setContactUrgence(String contactUrgence) { this.contactUrgence = contactUrgence; }
    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }
    public User getParent() { return parent; }
    public void setParent(User parent) { this.parent = parent; }
    public void setId(Long id) { this.id = id; }

    public boolean isArchive() {
        return archive;
    }

    public void setArchive(boolean archive) {
        this.archive = archive;
    }

}
