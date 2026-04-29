package com.tinyspring.garderie.dto.RH;

import com.tinyspring.garderie.entity.RH.enums.StatutAnimatrice;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnimatriceDTO {

    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Email invalide")
    private String email;

    // ✅ CORRIGÉ : ^$ autorise chaîne vide (champ optionnel)
    @Pattern(regexp = "^$|^[0-9]{8}$", message = "Téléphone invalide (8 chiffres requis)")
    private String telephone;

    private LocalDate dateEmbauche;

    private StatutAnimatrice statut;

    private String specialite;

    private String photoUrl;

    // Retourné uniquement lors de la création pour afficher à l'admin
    private String motDePasseTemporaire;
}