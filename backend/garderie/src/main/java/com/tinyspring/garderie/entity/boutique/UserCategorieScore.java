package com.tinyspring.garderie.entity.boutique;

import com.tinyspring.garderie.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_categorie_scores",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "categorie_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCategorieScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_id", nullable = false)
    private Categorie categorie;

    @Column(nullable = false)
    @Builder.Default
    private Double score = 0.0;

    @Column(name = "derniere_interaction")
    private LocalDateTime derniereInteraction;

    @Column(name = "nb_interactions")
    @Builder.Default
    private Integer nbInteractions = 0;
}