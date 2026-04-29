package com.tinyspring.garderie.service.boutique;

import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.entity.boutique.Commande;
import com.tinyspring.garderie.entity.boutique.CommandeProduit;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests EmailService")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private EmailServiceImpl emailService;

    private Commande commande;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "no-reply@example.com");
        commande = commande();
    }

    @Test
    @DisplayName("sendCommandeCreee() envoie un email HTML")
    void sendCommandeCreee_shouldSendHtmlEmail() throws Exception {
        MimeMessage message = mimeMessage();
        when(templateEngine.process(eq("emails/commande-creee"), any(Context.class)))
                .thenReturn("<html>commande creee</html>");
        when(mailSender.createMimeMessage()).thenReturn(message);

        emailService.sendCommandeCreee(commande);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("emails/commande-creee"), contextCaptor.capture());
        verify(mailSender).send(message);
        assertThat(contextCaptor.getValue().getVariable("nomParent")).isEqualTo("Parent Test");
        assertThat(contextCaptor.getValue().getVariable("commandeId")).isEqualTo(42L);
        assertThat(contextCaptor.getValue().getVariable("adresse")).isEqualTo("12 rue des Fleurs");
        assertThat(contextCaptor.getValue().getVariable("produits")).isEqualTo(commande.getItems());
        assertThat(contextCaptor.getValue().getVariable("montantTotal")).isEqualTo(120.0);
        assertThat(contextCaptor.getValue().getVariable("dateCommande")).isEqualTo("29/04/2026 10:30");
        assertThat(message.getSubject()).contains("Commande #42");
    }

    @Test
    @DisplayName("sendCommandeConfirmee() envoie le template confirme")
    void sendCommandeConfirmee_shouldSendConfirmedEmail() throws Exception {
        MimeMessage message = mimeMessage();
        when(templateEngine.process(eq("emails/commande-confirmee"), any(Context.class)))
                .thenReturn("<html>commande confirmee</html>");
        when(mailSender.createMimeMessage()).thenReturn(message);

        emailService.sendCommandeConfirmee(commande);

        verify(templateEngine).process(eq("emails/commande-confirmee"), any(Context.class));
        verify(mailSender).send(message);
        assertThat(message.getSubject()).contains("Commande #42");
    }

    @Test
    @DisplayName("sendCommandeLivree() envoie le template livre")
    void sendCommandeLivree_shouldSendDeliveredEmail() throws Exception {
        MimeMessage message = mimeMessage();
        when(templateEngine.process(eq("emails/commande-livree"), any(Context.class)))
                .thenReturn("<html>commande livree</html>");
        when(mailSender.createMimeMessage()).thenReturn(message);

        emailService.sendCommandeLivree(commande);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("emails/commande-livree"), contextCaptor.capture());
        verify(mailSender).send(message);
        assertThat(contextCaptor.getValue().getVariable("dateLivraison")).isNotNull();
        assertThat(message.getSubject()).contains("Commande #42");
    }

    @Test
    @DisplayName("envoyerEmailEchecPaiement() envoie un email avec les liens d'action")
    void envoyerEmailEchecPaiement_shouldSendFailureEmail() throws Exception {
        MimeMessage message = mimeMessage();
        when(templateEngine.process(eq("email-echec-paiement"), any(Context.class)))
                .thenReturn("<html>paiement echoue</html>");
        when(mailSender.createMimeMessage()).thenReturn(message);

        emailService.envoyerEmailEchecPaiement(
                commande,
                "http://front/especes",
                "http://front/refus"
        );

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email-echec-paiement"), contextCaptor.capture());
        verify(mailSender).send(message);
        assertThat(contextCaptor.getValue().getVariable("nomParent")).isEqualTo("Parent Test");
        assertThat(contextCaptor.getValue().getVariable("commandeId")).isEqualTo(42L);
        assertThat(contextCaptor.getValue().getVariable("nbArticles")).isEqualTo(1);
        assertThat(contextCaptor.getValue().getVariable("montant")).isEqualTo(120.0);
        assertThat(contextCaptor.getValue().getVariable("lienEspeces")).isEqualTo("http://front/especes");
        assertThat(contextCaptor.getValue().getVariable("lienRefus")).isEqualTo("http://front/refus");
        assertThat(message.getSubject()).contains("Commande #42");
    }

    @Test
    @DisplayName("notifierAdminPaiementEspeces() envoie un email simple a l'admin")
    void notifierAdminPaiementEspeces_shouldSendSimpleMailToAdmin() {
        emailService.notifierAdminPaiementEspeces(commande);

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("admin@garderie.com");
        assertThat(sent.getSubject()).contains("Commande #42");
        assertThat(sent.getText())
                .contains("Parent Test")
                .contains("120.0 TND");
    }

    @Test
    @DisplayName("sendCommandeCreee() ignore une erreur de template")
    void sendCommandeCreee_shouldNotThrowWhenTemplateFails() {
        when(templateEngine.process(eq("emails/commande-creee"), any(Context.class)))
                .thenThrow(new RuntimeException("template indisponible"));

        assertThatCode(() -> emailService.sendCommandeCreee(commande))
                .doesNotThrowAnyException();

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    private Commande commande() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 7L);
        user.setNom("Parent Test");
        user.setEmail("parent@example.com");
        user.setPassword("secret");
        CommandeProduit item = CommandeProduit.builder()
                .quantite(2)
                .prixUnitaire(60.0)
                .build();
        return Commande.builder()
                .id(42L)
                .dateCommande(LocalDateTime.of(2026, 4, 29, 10, 30))
                .statut("PENDING")
                .paymentStatus("PENDING")
                .montantTotal(120.0)
                .adresseLivraison("12 rue des Fleurs")
                .user(user)
                .items(new ArrayList<>(List.of(item)))
                .build();
    }

    private MimeMessage mimeMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }
}
