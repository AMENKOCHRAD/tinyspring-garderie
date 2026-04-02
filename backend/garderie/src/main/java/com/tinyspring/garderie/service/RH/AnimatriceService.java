package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.AnimatriceDTO;
import com.tinyspring.garderie.entity.RH.Animatrice;
import com.tinyspring.garderie.entity.RH.enums.StatutAnimatrice;
import com.tinyspring.garderie.repository.RH.AnimatriceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnimatriceService {

    private final AnimatriceRepository animatriceRepository;

    // ========== ADMIN ==========

    public List<AnimatriceDTO> getAllAnimatrices() {
        return animatriceRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public AnimatriceDTO getAnimatriceById(Long id) {
        Animatrice animatrice = animatriceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Animatrice non trouvée avec l'id : " + id));
        return toDTO(animatrice);
    }

    public AnimatriceDTO createAnimatrice(AnimatriceDTO dto) {
        if (animatriceRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email déjà utilisé : " + dto.getEmail());
        }
        Animatrice animatrice = toEntity(dto);
        animatrice.setStatut(StatutAnimatrice.ACTIVE);
        return toDTO(animatriceRepository.save(animatrice));
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
        if (!animatriceRepository.existsById(id)) {
            throw new EntityNotFoundException("Animatrice non trouvée avec l'id : " + id);
        }
        animatriceRepository.deleteById(id);
    }

    public List<AnimatriceDTO> getAnimatricesByStatut(StatutAnimatrice statut) {
        return animatriceRepository.findByStatut(statut)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ========== ANIMATRICE ==========

    public AnimatriceDTO updateMonProfil(Long id, AnimatriceDTO dto) {
        Animatrice animatrice = animatriceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Animatrice non trouvée"));
        animatrice.setEmail(dto.getEmail());
        animatrice.setTelephone(dto.getTelephone());
        animatrice.setPhotoUrl(dto.getPhotoUrl());
        return toDTO(animatriceRepository.save(animatrice));
    }

    // ========== MAPPING ==========

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