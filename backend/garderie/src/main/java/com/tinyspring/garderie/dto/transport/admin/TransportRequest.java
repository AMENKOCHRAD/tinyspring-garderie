package com.tinyspring.garderie.dto.transport.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TransportRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 3, max = 60, message = "Le nom doit contenir entre 3 et 60 caracteres")
    @Pattern(regexp = "^(?! ).*(?<! )$", message = "Le nom ne doit pas commencer ou finir par un espace")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ0-9\\s\\-()]+$", message = "Le nom contient des caracteres non autorises")
    private String nom;

    @NotBlank(message = "Le matricule est obligatoire")
    @Size(min = 4, max = 20, message = "Le matricule doit contenir entre 4 et 20 caracteres")
    @Pattern(regexp = "^(?! ).*(?<! )$", message = "Le matricule ne doit pas commencer ou finir par un espace")
    @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "Le matricule doit contenir uniquement lettres, chiffres ou tirets")
    private String matricule;

    @NotNull(message = "La capacite est obligatoire")
    @Min(value = 1, message = "La capacite doit etre superieure ou egale a 1")
    @Max(value = 100, message = "La capacite doit etre inferieure ou egale a 100")
    private Integer capacite;

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getMatricule() {
        return matricule;
    }

    public void setMatricule(String matricule) {
        this.matricule = matricule;
    }

    public Integer getCapacite() {
        return capacite;
    }

    public void setCapacite(Integer capacite) {
        this.capacite = capacite;
    }
}
