package com.tinyspring.garderie.entity.RH;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rapports_rh")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RapportRH {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Question posée en langage naturel
    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    // Type de rapport détecté
    @Column(name = "type_rapport")
    private String typeRapport;

    // Période couverte
    @Column(name = "periode")
    private String periode;

    // Contenu du rapport généré par Gemini
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String contenu;

    // Données brutes envoyées à Gemini (JSON)
    @Column(name = "donnees_contexte", columnDefinition = "LONGTEXT")
    private String donneesContexte;

    // Date de génération
    @Column(name = "date_generation", nullable = false)
    private LocalDateTime dateGeneration;

    @PrePersist
    public void prePersist() {
        this.dateGeneration = LocalDateTime.now();
    }
}