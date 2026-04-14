package com.tinyspring.garderie.controller.boutique;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.tinyspring.garderie.service.boutique.CommandeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stripe")
public class StripeWebhookController {

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    private final CommandeService commandeService;

    public StripeWebhookController(CommandeService commandeService) {
        this.commandeService = commandeService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().body("Signature invalide");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur parsing");
        }

        System.out.println("✅ Webhook reçu : " + event.getType());

        if ("checkout.session.completed".equals(event.getType())) {
            try {
                // ✅ Lire directement depuis le JSON brut — compatible Stripe API 2025
                JsonObject dataObject = JsonParser.parseString(payload)
                        .getAsJsonObject()
                        .getAsJsonObject("data")
                        .getAsJsonObject("object");

                String clientReferenceId = dataObject.has("client_reference_id")
                        && !dataObject.get("client_reference_id").isJsonNull()
                        ? dataObject.get("client_reference_id").getAsString()
                        : null;

                String sessionId = dataObject.has("id")
                        ? dataObject.get("id").getAsString() : null;

                String paymentIntent = dataObject.has("payment_intent")
                        && !dataObject.get("payment_intent").isJsonNull()
                        ? dataObject.get("payment_intent").getAsString() : null;

                System.out.println("✅ clientReferenceId : " + clientReferenceId);
                System.out.println("✅ sessionId : " + sessionId);
                System.out.println("✅ paymentIntent : " + paymentIntent);

                if (clientReferenceId == null || clientReferenceId.isEmpty()) {
                    System.err.println("❌ clientReferenceId manquant !");
                    return ResponseEntity.ok("ignored");
                }

                Session session = new Session();
                session.setId(sessionId);
                session.setPaymentIntent(paymentIntent);
                session.setClientReferenceId(clientReferenceId);

                commandeService.markAsPaid(Long.valueOf(clientReferenceId), session);
                System.out.println("✅ Commande #" + clientReferenceId + " → CONFIRMEE/PAID");

            } catch (Exception e) {
                System.err.println("❌ Erreur : " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.internalServerError().body("Erreur");
            }
        }

        if ("checkout.session.expired".equals(event.getType())) {
            try {
                JsonObject dataObject = JsonParser.parseString(payload)
                        .getAsJsonObject()
                        .getAsJsonObject("data")
                        .getAsJsonObject("object");

                if (dataObject.has("client_reference_id")
                        && !dataObject.get("client_reference_id").isJsonNull()) {
                    Long commandeId = Long.valueOf(
                            dataObject.get("client_reference_id").getAsString());
                    commandeService.updateStatut(commandeId, "ANNULEE");
                }
            } catch (Exception e) {
                System.err.println("Erreur session expirée : " + e.getMessage());
            }
        }

        return ResponseEntity.ok("received");
    }
}