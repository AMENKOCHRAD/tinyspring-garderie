package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.AnimatriceDTO;
import com.tinyspring.garderie.entity.RH.Animatrice;
import com.tinyspring.garderie.entity.RH.enums.StatutAnimatrice;
import com.tinyspring.garderie.entity.Role;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.repository.RH.AnimatriceRepository;
import com.tinyspring.garderie.repository.RoleRepository;
import com.tinyspring.garderie.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AnimatriceService {

    private final AnimatriceRepository animatriceRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final NotificationService notificationService;

    public AnimatriceService(AnimatriceRepository animatriceRepository,
                             FileStorageService fileStorageService,
                             UserRepository userRepository,
                             RoleRepository roleRepository,
                             PasswordEncoder passwordEncoder,
                             EmailService emailService,
                             NotificationService notificationService) {
        this.animatriceRepository = animatriceRepository;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.notificationService = notificationService;
    }

    public List<AnimatriceDTO> getAllAnimatrices() {
        return animatriceRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public AnimatriceDTO getAnimatriceById(Long id) {
        return toDTO(animatriceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Animatrice non trouvée avec l'id : " + id)));
    }

    public AnimatriceDTO createAnimatrice(AnimatriceDTO dto) {
        if (animatriceRepository.existsByEmail(dto.getEmail()))
            throw new RuntimeException("Email déjà utilisé : " + dto.getEmail());

        Animatrice animatrice = toEntity(dto);
        animatrice.setStatut(StatutAnimatrice.ACTIVE);
        Animatrice savedAnimatrice = animatriceRepository.save(animatrice);

        String motDePasseTemporaire = genererMotDePasse();

        Role roleAnimatrice = roleRepository.findByName(RoleName.ANIMATRICE)
                .orElseThrow(() -> new RuntimeException("Rôle ANIMATRICE non trouvé"));

        userRepository.save(new User(
                dto.getPrenom() + " " + dto.getNom(),
                dto.getEmail(),
                passwordEncoder.encode(motDePasseTemporaire),
                true,
                roleAnimatrice
        ));

        emailService.envoyerCredentiels(dto.getEmail(), dto.getPrenom(), dto.getNom(), dto.getEmail(), motDePasseTemporaire);

        // ✅ Notification temps réel
        notificationService.creerNotification(
                "👤 Nouvelle animatrice ajoutée : " + dto.getPrenom() + " " + dto.getNom(),
                "ANIMATRICE"
        );

        AnimatriceDTO result = toDTO(savedAnimatrice);
        result.setMotDePasseTemporaire(motDePasseTemporaire);
        return result;
    }

    public AnimatriceDTO updateAnimatrice(Long id, AnimatriceDTO dto) {
        Animatrice animatrice = animatriceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Animatrice non trouvée avec l'id : " + id));
        animatrice.setNom(dto.getNom());
        animatrice.setPrenom(dto.getPrenom());
        animatrice.setEmail(dto.getEmail());
        animatrice.setTelephone(dto.getTelephone());
        animatrice.setDateEmbauche(dto.getDateEmbauche());
        animatrice.setStatut(dto.getStatut());
        animatrice.setSpecialite(dto.getSpecialite());
        animatrice.setPhotoUrl(dto.getPhotoUrl());
        return toDTO(animatriceRepository.save(animatrice));
    }

    public void deleteAnimatrice(Long id) {
        Animatrice animatrice = animatriceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Animatrice non trouvée avec l'id : " + id));
        userRepository.findByEmail(animatrice.getEmail()).ifPresent(userRepository::delete);
        if (animatrice.getPhotoUrl() != null) {
            try { fileStorageService.deleteFile(animatrice.getPhotoUrl()); }
            catch (IOException e) { System.err.println("Erreur suppression photo : " + e.getMessage()); }
        }
        animatriceRepository.deleteById(id);
    }

    public List<AnimatriceDTO> getAnimatricesByStatut(StatutAnimatrice statut) {
        return animatriceRepository.findByStatut(statut).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public AnimatriceDTO uploadPhoto(Long id, MultipartFile file) throws IOException {
        Animatrice animatrice = animatriceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Animatrice non trouvée"));
        if (animatrice.getPhotoUrl() != null && !animatrice.getPhotoUrl().isEmpty())
            fileStorageService.deleteFile(animatrice.getPhotoUrl());
        String fileName = fileStorageService.saveFile(file);
        animatrice.setPhotoUrl(fileName);
        return toDTO(animatriceRepository.save(animatrice));
    }

    public AnimatriceDTO updateMonProfil(Long id, AnimatriceDTO dto) {
        Animatrice animatrice = animatriceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Animatrice non trouvée"));
        animatrice.setEmail(dto.getEmail());
        animatrice.setTelephone(dto.getTelephone());
        animatrice.setPhotoUrl(dto.getPhotoUrl());
        return toDTO(animatriceRepository.save(animatrice));
    }

    public AnimatriceDTO getAnimatriceByEmail(String email) {
        return toDTO(animatriceRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Animatrice non trouvée avec l'email : " + email)));
    }

    private String genererMotDePasse() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public AnimatriceDTO toDTO(Animatrice animatrice) {
        return AnimatriceDTO.builder()
                .id(animatrice.getId())
                .nom(animatrice.getNom())
                .prenom(animatrice.getPrenom())
                .email(animatrice.getEmail())
                .telephone(animatrice.getTelephone())
                .dateEmbauche(animatrice.getDateEmbauche())
                .statut(animatrice.getStatut())
                .specialite(animatrice.getSpecialite())
                .photoUrl(animatrice.getPhotoUrl())
                .build();
    }

    private Animatrice toEntity(AnimatriceDTO dto) {
        return Animatrice.builder()
                .nom(dto.getNom())
                .prenom(dto.getPrenom())
                .email(dto.getEmail())
                .telephone(dto.getTelephone())
                .dateEmbauche(dto.getDateEmbauche())
                .statut(dto.getStatut())
                .specialite(dto.getSpecialite())
                .photoUrl(dto.getPhotoUrl())
                .build();
    }
}