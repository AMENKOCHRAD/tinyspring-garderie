package com.tinyspring.garderie.dto;

public class EnfantResponseDTO {
    public Long id;
    public String nom;
    public String prenom;
    public String dateNaissance;
    public String groupeSanguin;
    public String allergies;
    public String contactUrgence;
    public String photo;
    public ParentDTO parent;

    public static class ParentDTO {
        public Long id;
        public String nom;
        public String email;

    }
}