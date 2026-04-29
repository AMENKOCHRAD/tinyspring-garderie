package com.tinyspring.garderie.service.boutique;

import com.tinyspring.garderie.entity.boutique.Commande;

public interface EmailService {
    void sendCommandeCreee(Commande commande);
    void sendCommandeConfirmee(Commande commande);
    void sendCommandeLivree(Commande commande);
    void envoyerEmailEchecPaiement(Commande commande,
                                   String lienEspeces, String lienRefus);
    void notifierAdminPaiementEspeces(Commande commande);
}