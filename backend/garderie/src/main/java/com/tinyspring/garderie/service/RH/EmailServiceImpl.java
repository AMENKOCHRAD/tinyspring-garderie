package com.tinyspring.garderie.service.RH;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements IEmailService {

    private final JavaMailSender mailSender;

    @Override
    public void envoyerCredentiels(String destinataire, String prenom, String nom,
                                   String email, String motDePasse) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(destinataire);
            helper.setSubject("🌸 Bienvenue à TinySpring — Vos identifiants de connexion");
            helper.setText(construireCorpsEmail(prenom, nom, email, motDePasse), true);

            mailSender.send(message);
            System.out.println("✅ Email envoyé à : " + destinataire);

        } catch (MessagingException e) {
            System.err.println("❌ Erreur construction email : " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email SMTP : " + e.getMessage());
        }
    }

    @Override
    public void envoyerDecisionAbsence(String destinataire, String prenom, String nom,
                                       String type, String dateDebut, String dateFin,
                                       boolean approuve, String motifRefus) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(destinataire);
            String sujet = approuve
                    ? "✅ Votre demande d'absence a été approuvée"
                    : "❌ Votre demande d'absence a été refusée";
            helper.setSubject(sujet);
            helper.setText(construireCorpsDecision(prenom, nom, type, dateDebut, dateFin, approuve, motifRefus), true);

            mailSender.send(message);
            System.out.println("✅ Email décision envoyé à : " + destinataire);

        } catch (MessagingException e) {
            System.err.println("❌ Erreur construction email décision : " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email décision SMTP : " + e.getMessage());
        }
    }

    private String construireCorpsEmail(String prenom, String nom,
                                        String email, String motDePasse) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;
                            border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden;">
                  <div style="background: #4f46e5; padding: 30px; text-align: center;">
                    <h1 style="color: white; margin: 0; font-size: 24px;">🌸 TinySpring Garderie</h1>
                    <p style="color: #c7d2fe; margin: 8px 0 0;">Système de gestion interne</p>
                  </div>
                  <div style="padding: 32px;">
                    <h2 style="color: #1e293b; margin-top: 0;">Bienvenue, %s %s !</h2>
                    <p style="color: #475569;">Votre compte animatrice a été créé avec succès.</p>
                    <div style="background: #f8fafc; border-left: 4px solid #4f46e5; padding: 20px; border-radius: 8px; margin: 24px 0;">
                      <p style="margin: 0 0 10px; color: #64748b; font-size: 13px;">📧 <strong>Email :</strong> %s</p>
                      <p style="margin: 0; color: #64748b; font-size: 13px;">🔑 <strong>Mot de passe temporaire :</strong>
                        <span style="background: #e0e7ff; color: #4f46e5; padding: 2px 8px; border-radius: 4px; font-family: monospace;">%s</span>
                      </p>
                    </div>
                    <p style="color: #94a3b8; font-size: 12px; margin-top: 32px; text-align: center;">
                      © 2026 TinySpring — Tous droits réservés
                    </p>
                  </div>
                </div>
                """.formatted(prenom, nom, email, motDePasse);
    }

    private String construireCorpsDecision(String prenom, String nom, String type,
                                           String dateDebut, String dateFin,
                                           boolean approuve, String motifRefus) {
        String couleur = approuve ? "#10b981" : "#ef4444";
        String icone = approuve ? "✅" : "❌";
        String statut = approuve ? "APPROUVÉE" : "REFUSÉE";

        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;
                        border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden;">
              <div style="background: %s; padding: 30px; text-align: center;">
                <h1 style="color: white; margin: 0; font-size: 22px;">%s Demande %s</h1>
              </div>
              <div style="padding: 32px;">
                <p style="color: #475569;">Bonjour <strong>%s %s</strong>,</p>
                <div style="background: #f8fafc; border-left: 4px solid %s; padding: 20px; border-radius: 8px; margin: 24px 0;">
                  <p style="margin: 0 0 8px; color: #64748b; font-size: 13px;">📋 <strong>Type :</strong> %s</p>
                  <p style="margin: 0 0 8px; color: #64748b; font-size: 13px;">📅 <strong>Du :</strong> %s</p>
                  <p style="margin: 0; color: #64748b; font-size: 13px;">📅 <strong>Au :</strong> %s</p>
                </div>
                <p style="color: #94a3b8; font-size: 12px; margin-top: 32px; text-align: center;">© 2026 TinySpring Garderie</p>
              </div>
            </div>
            """.formatted(couleur, icone, statut, prenom, nom, couleur, type, dateDebut, dateFin);
    }
}
