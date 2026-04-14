package com.tinyspring.garderie.service.boutique;

import com.tinyspring.garderie.entity.boutique.Commande;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromAddress;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendCommandeCreee(Commande commande) {
        Context ctx = buildContext(commande);
        ctx.setVariable("dateCommande",
                commande.getDateCommande().format(FMT));
        sendEmail(
                commande.getUser().getEmail(),
                "✅ Commande #" + commande.getId() + " reçue — TinySpring",
                "emails/commande-creee",
                ctx
        );
    }

    @Async
    public void sendCommandeConfirmee(Commande commande) {
        Context ctx = buildContext(commande);
        ctx.setVariable("dateCommande",
                commande.getDateCommande().format(FMT));
        sendEmail(
                commande.getUser().getEmail(),
                "🎯 Commande #" + commande.getId() + " confirmée — TinySpring",
                "emails/commande-confirmee",
                ctx
        );
    }

    @Async
    public void sendCommandeLivree(Commande commande) {
        Context ctx = buildContext(commande);
        ctx.setVariable("dateLivraison",
                java.time.LocalDateTime.now().format(FMT));
        sendEmail(
                commande.getUser().getEmail(),
                "📦 Commande #" + commande.getId() + " livrée — TinySpring",
                "emails/commande-livree",
                ctx
        );
    }

    private Context buildContext(Commande commande) {
        Context ctx = new Context();
        ctx.setVariable("nomParent",   commande.getUser().getNom());
        ctx.setVariable("commandeId",  commande.getId());
        ctx.setVariable("adresse",     commande.getAdresseLivraison());
        ctx.setVariable("produits",    commande.getItems());
        ctx.setVariable("montantTotal",commande.getMontantTotal());
        return ctx;
    }

    private void sendEmail(String to, String subject, String template, Context ctx) {
        try {
            String html = templateEngine.process(template, ctx);

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            System.out.println("=== ENVOI EMAIL ===");
            System.out.println("FROM : " + fromAddress);
            System.out.println("TO   : " + to);
            System.out.println("SUBJ : " + subject);
            System.out.println("TPL  : " + template);

            mailSender.send(msg);

            System.out.println("=== EMAIL ENVOYE AVEC SUCCES ===");

        } catch (Exception e) {
            System.err.println("=== ERREUR ENVOI EMAIL ===");
            e.printStackTrace();
        }
    }
}