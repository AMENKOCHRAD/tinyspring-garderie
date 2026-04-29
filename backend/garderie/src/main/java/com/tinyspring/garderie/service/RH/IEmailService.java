package com.tinyspring.garderie.service.RH;

public interface IEmailService {

    void envoyerCredentiels(String destinataire, String prenom, String nom,
                            String email, String motDePasse);

    void envoyerDecisionAbsence(String destinataire, String prenom, String nom,
                                String type, String dateDebut, String dateFin,
                                boolean approuve, String motifRefus);
}
