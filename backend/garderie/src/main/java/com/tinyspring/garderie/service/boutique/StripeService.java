package com.tinyspring.garderie.service.boutique;

import com.tinyspring.garderie.entity.boutique.Commande;
import com.tinyspring.garderie.entity.boutique.CommandeProduit;
import java.util.List;
import java.util.Map;

public interface StripeService {
    Map<String, String> createCheckoutSession(Commande commande,
                                              List<CommandeProduit> items);
    void createRefund(String paymentIntentId);
}