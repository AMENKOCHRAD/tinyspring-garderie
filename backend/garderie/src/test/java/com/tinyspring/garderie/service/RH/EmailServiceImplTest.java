package com.tinyspring.garderie.service.RH;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests EmailServiceImpl")
class EmailServiceImplTest {

    @Mock private JavaMailSender mailSender;
    @Mock private MimeMessage mimeMessage;
    @InjectMocks private EmailServiceImpl service;

    @BeforeEach
    void setUp() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    // ===== envoyerCredentiels =====

    @Test
    @DisplayName("envoyerCredentiels — doit créer et envoyer le message")
    void envoyerCredentiels_doitEnvoyer() {
        service.envoyerCredentiels(
                "sara@tinyspring.com", "Sara", "Ben Ali",
                "sara@tinyspring.com", "temp123"
        );

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("envoyerCredentiels — doit gérer l'exception SMTP sans lever d'erreur")
    void envoyerCredentiels_doitGererException() {
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

        // Ne doit pas lever d'exception — erreur catchée en interne
        assertThatCode(() -> service.envoyerCredentiels(
                "sara@tinyspring.com", "Sara", "Ben Ali",
                "sara@tinyspring.com", "temp123"
        )).doesNotThrowAnyException();
    }

    // ===== envoyerDecisionAbsence — APPROUVÉ =====

    @Test
    @DisplayName("envoyerDecisionAbsence — doit envoyer email d'approbation")
    void envoyerDecisionAbsence_approuve() {
        service.envoyerDecisionAbsence(
                "sara@tinyspring.com", "Sara", "Ben Ali",
                "CONGE_ANNUEL", "2026-05-01", "2026-05-10",
                true, null
        );

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    // ===== envoyerDecisionAbsence — REFUSÉ =====

    @Test
    @DisplayName("envoyerDecisionAbsence — doit envoyer email de refus avec motif")
    void envoyerDecisionAbsence_refuse() {
        service.envoyerDecisionAbsence(
                "sara@tinyspring.com", "Sara", "Ben Ali",
                "CONGE_ANNUEL", "2026-05-01", "2026-05-10",
                false, "Quota dépassé"
        );

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("envoyerDecisionAbsence — doit gérer exception sans lever d'erreur")
    void envoyerDecisionAbsence_doitGererException() {
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> service.envoyerDecisionAbsence(
                "sara@tinyspring.com", "Sara", "Ben Ali",
                "ABSENCE", "2026-05-01", "2026-05-02",
                false, "Effectif insuffisant"
        )).doesNotThrowAnyException();
    }
}
