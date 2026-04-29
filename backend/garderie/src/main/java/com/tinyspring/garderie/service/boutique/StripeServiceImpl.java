package com.tinyspring.garderie.service.boutique;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.tinyspring.garderie.entity.boutique.Commande;
import com.tinyspring.garderie.entity.boutique.CommandeProduit;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StripeServiceImpl implements StripeService {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${stripe.currency:eur}")
    private String currency;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    @Override
    public Map<String, String> createCheckoutSession(Commande commande,
                                                     List<CommandeProduit> items) {
        try {
            List<SessionCreateParams.LineItem> lineItems = items.stream()
                    .map(item -> SessionCreateParams.LineItem.builder()
                            .setQuantity((long) item.getQuantite())
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency(currency)
                                    .setUnitAmount(Math.round(
                                            item.getPrixUnitaire() * 100))
                                    .setProductData(SessionCreateParams.LineItem
                                            .PriceData.ProductData.builder()
                                            .setName(item.getProduit().getNom())
                                            .build())
                                    .build())
                            .build())
                    .collect(Collectors.toList());

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(frontendUrl
                            + "/parent/boutique/paiement/success"
                            + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontendUrl
                            + "/parent/boutique/paiement/cancel"
                            + "?commande_id=" + commande.getId())
                    .setClientReferenceId(String.valueOf(commande.getId()))
                    .addAllLineItem(lineItems)
                    .build();

            Session session = Session.create(params);
            return Map.of("url", session.getUrl(), "sessionId", session.getId());

        } catch (StripeException e) {
            throw new RuntimeException("Erreur Stripe : " + e.getMessage());
        }
    }

    @Override
    public void createRefund(String paymentIntentId) {
        try {
            Refund.create(RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId).build());
        } catch (StripeException e) {
            throw new RuntimeException(
                    "Erreur remboursement Stripe : " + e.getMessage());
        }
    }
}