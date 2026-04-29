package com.tinyspring.garderie.service.boutique;

import com.tinyspring.garderie.entity.boutique.Commande;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.time.format.DateTimeFormatter;

@Service("boutiqueEmailServiceImpl")
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromAddress;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    @Async
    public void sendCommandeCreee(Commande commande) {
        Context ctx = buildContext(commande);
        ctx.setVariable("dateCommande",
                commande.getDateCommande().format(FMT));
        sendEmail(commande.getUser().getEmail(),
                "✅ Commande #" + commande.getId() + " reçue — TinySpring",
                "emails/commande-creee", ctx);
    }

    @Override
    @Async
    public void sendCommandeConfirmee(Commande commande) {
        Context ctx = buildContext(commande);
        ctx.setVariable("dateCommande",
                commande.getDateCommande().format(FMT));
        sendEmail(commande.getUser().getEmail(),
                "🎯 Commande #" + commande.getId() + " confirmée — TinySpring",
                "emails/commande-confirmee", ctx);
    }

    @Override
    @Async
    public void sendCommandeLivree(Commande commande) {
        Context ctx = buildContext(commande);
        ctx.setVariable("dateLivraison",
                java.time.LocalDateTime.now().format(FMT));
        sendEmail(commande.getUser().getEmail(),
                "📦 Commande #" + commande.getId() + " livrée — TinySpring",
                "emails/commande-livree", ctx);
    }

    @Override
    public void envoyerEmailEchecPaiement(Commande commande,
                                          String lienEspeces, String lienRefus) {
        Context ctx = new Context();
        ctx.setVariable("nomParent",  commande.getUser().getNom());
        ctx.setVariable("commandeId", commande.getId());
        ctx.setVariable("nbArticles", commande.getItems().size());
        ctx.setVariable("montant",    commande.getMontantTotal());
        ctx.setVariable("lienEspeces", lienEspeces);
        ctx.setVariable("lienRefus",   lienRefus);
        String html = templateEngine.process("email-echec-paiement", ctx);
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(commande.getUser().getEmail());
            helper.setFrom(fromAddress);
            helper.setSubject("⚠️ Paiement non abouti — Commande #" + commande.getId());
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erreur email : " + e.getMessage());
        }
    }

    @Override
    public void notifierAdminPaiementEspeces(Commande commande) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo("admin@garderie.com");
        msg.setSubject("💰 Paiement espèces — Commande #" + commande.getId());
        msg.setText("Le parent " + commande.getUser().getNom()
                + " a choisi de payer en espèces.\n"
                + "Commande #" + commande.getId()
                + " — " + commande.getMontantTotal() + " TND\n"
                + "Action requise : livrer et marquer comme payée.");
        mailSender.send(msg);
    }

    private Context buildContext(Commande commande) {
        Context ctx = new Context();
        ctx.setVariable("nomParent",    commande.getUser().getNom());
        ctx.setVariable("commandeId",   commande.getId());
        ctx.setVariable("adresse",      commande.getAdresseLivraison());
        ctx.setVariable("produits",     commande.getItems());
        ctx.setVariable("montantTotal", commande.getMontantTotal());
        return ctx;
    }

    private void sendEmail(String to, String subject,
                           String template, Context ctx) {
        try {
            String html = templateEngine.process(template, ctx);
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
        } catch (Exception e) {
            System.err.println("Erreur envoi email : " + e.getMessage());
        }
    }
}
