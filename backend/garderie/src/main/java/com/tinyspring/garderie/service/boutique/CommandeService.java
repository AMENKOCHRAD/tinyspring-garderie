package com.tinyspring.garderie.service.boutique;

import com.stripe.model.checkout.Session;
import com.tinyspring.garderie.dto.boutique.CommandeDto;
import com.tinyspring.garderie.dto.boutique.CommandeRequest;
import java.util.List;
import java.util.Map;

public interface CommandeService {
    List<CommandeDto> findAll();
    CommandeDto findById(Long id);
    List<CommandeDto> findByUser(Long userId);
    List<CommandeDto> findByStatut(String statut);
    CommandeDto create(CommandeRequest request);
    Map<String, String> createCheckoutSession(Long commandeId);
    void markAsPaid(Long commandeId, Session session);
    CommandeDto updateStatut(Long id, String nouveauStatut);
    void delete(Long id);
    void envoyerEmailEchecPaiement(Long commandeId);
    void accepterPaiementEspeces(String token);
    void refuserEtAnnuler(String token);
}