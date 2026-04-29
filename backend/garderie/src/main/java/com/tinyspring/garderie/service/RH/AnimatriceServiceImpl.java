package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.AnimatriceDTO;
import com.tinyspring.garderie.dto.RH.mapper.AnimatriceMapper;
import com.tinyspring.garderie.entity.RH.Animatrice;
import com.tinyspring.garderie.entity.RH.enums.StatutAnimatrice;
import com.tinyspring.garderie.entity.Role;
import com.tinyspring.garderie.entity.RoleName;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.repository.RH.AnimatriceFormationRepository;
import com.tinyspring.garderie.repository.RH.AnimatriceRepository;
import com.tinyspring.garderie.repository.RH.HistoriqueDecisionRepository;
import com.tinyspring.garderie.repository.RoleRepository;
import com.tinyspring.garderie.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AnimatriceServiceImpl implements IAnimatriceService {

    private final AnimatriceRepository animatriceRepository;
    private final IFileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final IEmailService emailService;
    private final INotificationService notificationService;
    private final AnimatriceMapper animatriceMapper;
    private final AuthenticationManager authenticationManager;
    private final HistoriqueDecisionRepository historiqueDecisionRepository;
    private final AnimatriceFormationRepository animatriceFormationRepository;

    public AnimatriceServiceImpl(AnimatriceRepository animatriceRepository,
                                 IFileStorageService fileStorageService,
                                 UserRepository userRepository,
                                 RoleRepository roleRepository,
                                 PasswordEncoder passwordEncoder,
                                 IEmailService emailService,
                                 INotificationService notificationService,
                                 AnimatriceMapper animatriceMapper,
                                 AuthenticationManager authenticationManager,
                                 HistoriqueDecisionRepository historiqueDecisionRepository,
                                 AnimatriceFormationRepository animatriceFormationRepository) {
        this.animatriceRepository         = animatriceRepository;
        this.fileStorageService           = fileStorageService;
        this.userRepository               = userRepository;
        this.roleRepository               = roleRepository;
        this.passwordEncoder              = passwordEncoder;
        this.emailService                 = emailService;
        this.notificationService          = notificationService;
        this.animatriceMapper             = animatriceMapper;
        this.authenticationManager        = authenticationManager;
        this.historiqueDecisionRepository  = historiqueDecisionRepository;
        this.animatriceFormationRepository = animatriceFormationRepository;
    }

    @Override
    public List<AnimatriceDTO> getAllAnimatrices() {
        return animatriceRepository.findAll().stream()
                .map(animatriceMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AnimatriceDTO getAnimatriceById(Long id) {
        return animatriceMapper.toDTO(animatriceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Animatrice non trouvée avec l'id : " + id)));
    }

    @Override
    public AnimatriceDTO getAnimatriceByEmail(String email) {
        return animatriceMapper.toDTO(animatriceRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Animatrice non trouvée avec l'email : " + email)));
    }

    @Override
    public AnimatriceDTO createAnimatrice(AnimatriceDTO dto) {
        if (animatriceRepository.existsByEmail(dto.getEmail()))
            throw new RuntimeException("Email déjà utilisé : " + dto.getEmail());

        Animatrice animatrice = animatriceMapper.toEntity(dto);
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

        emailService.envoyerCredentiels(
                dto.getEmail(), dto.getPrenom(), dto.getNom(),
                dto.getEmail(), motDePasseTemporaire
        );

        notificationService.creerNotification(
                "👤 Nouvelle animatrice ajoutée : " + dto.getPrenom() + " " + dto.getNom(),
                "ANIMATRICE"
        );

        AnimatriceDTO result = animatriceMapper.toDTO(savedAnimatrice);
        result.setMotDePasseTemporaire(motDePasseTemporaire);
        return result;
    }

    @Override
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

        return animatriceMapper.toDTO(animatriceRepository.save(animatrice));
    }

    @Override
    public AnimatriceDTO updateMonProfil(Long id, AnimatriceDTO dto) {
        Animatrice animatrice = animatriceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Animatrice non trouvée"));

        animatrice.setNom(dto.getNom());
        animatrice.setPrenom(dto.getPrenom());
        animatrice.setEmail(dto.getEmail());
        animatrice.setTelephone(dto.getTelephone());
        animatrice.setSpecialite(dto.getSpecialite());
        animatrice.setPhotoUrl(dto.getPhotoUrl());

        userRepository.findByEmail(animatrice.getEmail()).ifPresent(user -> {
            user.setNom(dto.getPrenom() + " " + dto.getNom());
            userRepository.save(user);
        });

        return animatriceMapper.toDTO(animatriceRepository.save(animatrice));
    }

    @Override
    @Transactional
    public void deleteAnimatrice(Long id) {
        Animatrice animatrice = animatriceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Animatrice non trouvée avec l'id : " + id));

        // ✅ 1. Supprimer les historiques de décision (référencent absence_conges)
        historiqueDecisionRepository.deleteByAbsenceCongeAnimatriceId(id);

        // ✅ 2. Supprimer les inscriptions aux formations (référencent animatrices)
        animatriceFormationRepository.deleteByAnimatriceId(id);

        // ✅ 3. Supprimer le compte utilisateur
        userRepository.findByEmail(animatrice.getEmail()).ifPresent(userRepository::delete);

        // ✅ 4. Supprimer la photo
        if (animatrice.getPhotoUrl() != null) {
            try { fileStorageService.deleteFile(animatrice.getPhotoUrl()); }
            catch (IOException e) { System.err.println("Erreur suppression photo : " + e.getMessage()); }
        }

        // ✅ 5. Supprimer l'animatrice (cascade supprime les absence_conges)
        animatriceRepository.deleteById(id);
    }

    @Override
    public List<AnimatriceDTO> getAnimatricesByStatut(StatutAnimatrice statut) {
        return animatriceRepository.findByStatut(statut).stream()
                .map(animatriceMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AnimatriceDTO uploadPhoto(Long id, MultipartFile file) throws IOException {
        Animatrice animatrice = animatriceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Animatrice non trouvée"));

        if (animatrice.getPhotoUrl() != null && !animatrice.getPhotoUrl().isEmpty())
            fileStorageService.deleteFile(animatrice.getPhotoUrl());

        String fileName = fileStorageService.saveFile(file);
        animatrice.setPhotoUrl(fileName);
        return animatriceMapper.toDTO(animatriceRepository.save(animatrice));
    }

    @Override
    public boolean mustChangePassword(Long id) {
        Animatrice animatrice = animatriceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Animatrice non trouvée"));
        return animatrice.isMustChangePassword();
    }

    @Override
    public void changerMotDePasse(Long id, String ancienMotDePasse, String nouveauMotDePasse) {
        Animatrice animatrice = animatriceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Animatrice non trouvée"));

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(animatrice.getEmail(), ancienMotDePasse)
            );
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Ancien mot de passe incorrect.");
        }

        if (nouveauMotDePasse.length() < 6)
            throw new RuntimeException("Le nouveau mot de passe doit contenir au moins 6 caractères.");

        User user = userRepository.findByEmail(animatrice.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Compte utilisateur non trouvé"));
        user.setPassword(passwordEncoder.encode(nouveauMotDePasse));
        userRepository.save(user);

        animatrice.setMustChangePassword(false);
        animatriceRepository.save(animatrice);
    }

    private String genererMotDePasse() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}