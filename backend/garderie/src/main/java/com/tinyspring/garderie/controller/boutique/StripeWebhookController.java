package com.tinyspring.garderie.controller.boutique;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.tinyspring.garderie.service.boutique.CommandeService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

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

        // ── Paiement réussi ───────────────────────────────────────────────────
        if ("checkout.session.completed".equals(event.getType())) {
            try {
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

        // ── Session expirée → Email avec boutons espèces/refus ────────────────
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

                    System.out.println("⚠️ Session expirée — Commande #" + commandeId);

                    // ✅ Envoyer l'email avec les 2 boutons espèces/refus
                    commandeService.envoyerEmailEchecPaiement(commandeId);

                    System.out.println("📧 Email échec paiement envoyé — Commande #" + commandeId);
                }
            } catch (Exception e) {
                System.err.println("Erreur session expirée : " + e.getMessage());
                e.printStackTrace();
            }
        }

        return ResponseEntity.ok("received");
    }
}