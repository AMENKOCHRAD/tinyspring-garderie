package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.AnimatriceDTO;
import com.tinyspring.garderie.dto.RH.NotificationDTO;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests AnimatriceServiceImpl")
class AnimatriceServiceImplTest {

    @Mock private AnimatriceRepository animatriceRepository;
    @Mock private IFileStorageService fileStorageService;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private IEmailService emailService;
    @Mock private INotificationService notificationService;
    @Mock private AnimatriceMapper animatriceMapper;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private HistoriqueDecisionRepository historiqueDecisionRepository;
    @Mock private AnimatriceFormationRepository animatriceFormationRepository;

    @InjectMocks private AnimatriceServiceImpl service;

    private Animatrice animatrice;
    private AnimatriceDTO animatriceDTO;

    @BeforeEach
    void setUp() {
        animatrice = new Animatrice();
        animatrice.setId(1L);
        animatrice.setNom("Ben Ali");
        animatrice.setPrenom("Sara");
        animatrice.setEmail("sara@tinyspring.com");
        animatrice.setStatut(StatutAnimatrice.ACTIVE);

        animatriceDTO = new AnimatriceDTO();
        animatriceDTO.setNom("Ben Ali");
        animatriceDTO.setPrenom("Sara");
        animatriceDTO.setEmail("sara@tinyspring.com");
        animatriceDTO.setStatut(StatutAnimatrice.ACTIVE);
    }

    // ===== getAllAnimatrices =====
    @Test
    @DisplayName("getAllAnimatrices — doit retourner la liste complète")
    void getAllAnimatrices_doitRetournerListe() {
        when(animatriceRepository.findAll()).thenReturn(List.of(animatrice));
        when(animatriceMapper.toDTO(any())).thenReturn(animatriceDTO);

        List<AnimatriceDTO> result = service.getAllAnimatrices();

        assertThat(result).hasSize(1);
    }

    // ===== getAnimatriceById =====
    @Test
    @DisplayName("getAnimatriceById — doit retourner le DTO si trouvé")
    void getAnimatriceById_doitRetournerDTO() {
        when(animatriceRepository.findById(1L)).thenReturn(Optional.of(animatrice));
        when(animatriceMapper.toDTO(any())).thenReturn(animatriceDTO);

        AnimatriceDTO result = service.getAnimatriceById(1L);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getAnimatriceById — doit lever exception si introuvable")
    void getAnimatriceById_doitLeverException() {
        when(animatriceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAnimatriceById(99L))
                .isInstanceOf(EntityNotFoundException.class).hasMessageContaining("99");
    }

    // ===== getAnimatriceByEmail =====
    @Test
    @DisplayName("getAnimatriceByEmail — doit retourner le DTO si trouvé")
    void getAnimatriceByEmail_doitRetournerDTO() {
        when(animatriceRepository.findByEmail("sara@tinyspring.com"))
                .thenReturn(Optional.of(animatrice));
        when(animatriceMapper.toDTO(any())).thenReturn(animatriceDTO);

        AnimatriceDTO result = service.getAnimatriceByEmail("sara@tinyspring.com");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getAnimatriceByEmail — doit lever exception si introuvable")
    void getAnimatriceByEmail_doitLeverException() {
        when(animatriceRepository.findByEmail("inconnu@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAnimatriceByEmail("inconnu@test.com"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ===== createAnimatrice =====
    @Test
    @DisplayName("createAnimatrice — doit créer avec statut ACTIVE et envoyer email")
    void createAnimatrice_doitCreerEtEnvoyerEmail() {
        Role role = new Role();
        role.setName(RoleName.ANIMATRICE);

        when(animatriceRepository.existsByEmail(any())).thenReturn(false);
        when(animatriceMapper.toEntity(any())).thenReturn(animatrice);
        when(animatriceRepository.save(any())).thenReturn(animatrice);
        when(roleRepository.findByName(RoleName.ANIMATRICE)).thenReturn(Optional.of(role));
        when(userRepository.save(any())).thenReturn(new User());
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(animatriceMapper.toDTO(any())).thenReturn(animatriceDTO);
        doNothing().when(emailService).envoyerCredentiels(any(), any(), any(), any(), any());
        when(notificationService.creerNotification(any(), any())).thenReturn(new NotificationDTO());

        service.createAnimatrice(animatriceDTO);

        assertThat(animatrice.getStatut()).isEqualTo(StatutAnimatrice.ACTIVE);
        verify(emailService).envoyerCredentiels(any(), any(), any(), any(), any());
        verify(notificationService).creerNotification(any(), any());
    }

    @Test
    @DisplayName("createAnimatrice — doit lever exception si email déjà utilisé")
    void createAnimatrice_doitLeverExceptionSiEmailExistant() {
        when(animatriceRepository.existsByEmail("sara@tinyspring.com")).thenReturn(true);

        assertThatThrownBy(() -> service.createAnimatrice(animatriceDTO))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("Email déjà utilisé");
    }

    // ===== updateAnimatrice =====
    @Test
    @DisplayName("updateAnimatrice — doit mettre à jour les champs")
    void updateAnimatrice_doitMettreAJour() {
        when(animatriceRepository.findById(1L)).thenReturn(Optional.of(animatrice));
        when(animatriceRepository.save(any())).thenReturn(animatrice);
        when(animatriceMapper.toDTO(any())).thenReturn(animatriceDTO);

        AnimatriceDTO dto = new AnimatriceDTO();
        dto.setNom("Nouveau Nom");
        dto.setPrenom("Nouveau Prenom");
        dto.setEmail("nouveau@test.com");
        dto.setStatut(StatutAnimatrice.ACTIVE);

        AnimatriceDTO result = service.updateAnimatrice(1L, dto);

        verify(animatriceRepository).save(any());
        assertThat(animatrice.getNom()).isEqualTo("Nouveau Nom");
    }

    @Test
    @DisplayName("updateAnimatrice — doit lever exception si introuvable")
    void updateAnimatrice_doitLeverException() {
        when(animatriceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAnimatrice(99L, animatriceDTO))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ===== getAnimatricesByStatut =====
    @Test
    @DisplayName("getAnimatricesByStatut — doit filtrer par statut")
    void getAnimatricesByStatut_doitFiltrerParStatut() {
        when(animatriceRepository.findByStatut(StatutAnimatrice.ACTIVE))
                .thenReturn(List.of(animatrice));
        when(animatriceMapper.toDTO(any())).thenReturn(animatriceDTO);

        List<AnimatriceDTO> result = service.getAnimatricesByStatut(StatutAnimatrice.ACTIVE);

        assertThat(result).hasSize(1);
    }

    // ===== mustChangePassword =====
    @Test
    @DisplayName("mustChangePassword — doit retourner true pour nouveau compte")
    void mustChangePassword_doitRetournerTrue() {
        animatrice.setMustChangePassword(true);
        when(animatriceRepository.findById(1L)).thenReturn(Optional.of(animatrice));

        assertThat(service.mustChangePassword(1L)).isTrue();
    }

    @Test
    @DisplayName("mustChangePassword — doit retourner false après changement")
    void mustChangePassword_doitRetournerFalse() {
        animatrice.setMustChangePassword(false);
        when(animatriceRepository.findById(1L)).thenReturn(Optional.of(animatrice));

        assertThat(service.mustChangePassword(1L)).isFalse();
    }

    // ===== changerMotDePasse =====
    @Test
    @DisplayName("changerMotDePasse — doit lever exception si ancien MDP incorrect")
    void changerMotDePasse_doitLeverExceptionSiAncienMdpIncorrect() {
        when(animatriceRepository.findById(1L)).thenReturn(Optional.of(animatrice));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> service.changerMotDePasse(1L, "mauvais", "nouveau123"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("Ancien mot de passe incorrect");
    }

    @Test
    @DisplayName("changerMotDePasse — doit lever exception si nouveau MDP trop court")
    void changerMotDePasse_doitLeverExceptionSiNouveauMdpTropCourt() {
        when(animatriceRepository.findById(1L)).thenReturn(Optional.of(animatrice));
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken("sara@tinyspring.com", "ok"));

        assertThatThrownBy(() -> service.changerMotDePasse(1L, "ancienOk", "abc"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("6 caractères");
    }

    @Test
    @DisplayName("changerMotDePasse — doit réussir et mettre mustChangePassword à false")
    void changerMotDePasse_doitReussir() {
        User user = new User();
        animatrice.setMustChangePassword(true);

        when(animatriceRepository.findById(1L)).thenReturn(Optional.of(animatrice));
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken("sara@tinyspring.com", "ancienOk"));
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(animatriceRepository.save(any())).thenReturn(animatrice);

        service.changerMotDePasse(1L, "ancienOk", "nouveau123");

        assertThat(animatrice.isMustChangePassword()).isFalse();
        verify(userRepository).save(any());
    }

    // ===== deleteAnimatrice =====
    @Test
    @DisplayName("deleteAnimatrice — doit lever exception si introuvable")
    void deleteAnimatrice_doitLeverException() {
        when(animatriceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteAnimatrice(99L))
                .isInstanceOf(EntityNotFoundException.class).hasMessageContaining("99");
    }

    @Test
    @DisplayName("deleteAnimatrice — doit supprimer l'animatrice et ses dépendances")
    void deleteAnimatrice_doitSupprimerAnimatrice() {
        animatrice.setPhotoUrl(null);
        when(animatriceRepository.findById(1L)).thenReturn(Optional.of(animatrice));
        doNothing().when(historiqueDecisionRepository).deleteByAbsenceCongeAnimatriceId(1L);
        doNothing().when(animatriceFormationRepository).deleteByAnimatriceId(1L);
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        doNothing().when(animatriceRepository).deleteById(1L);

        service.deleteAnimatrice(1L);

        verify(animatriceRepository).deleteById(1L);
        verify(historiqueDecisionRepository).deleteByAbsenceCongeAnimatriceId(1L);
        verify(animatriceFormationRepository).deleteByAnimatriceId(1L);
    }
}