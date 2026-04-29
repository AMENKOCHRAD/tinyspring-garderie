package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.EnfantDTO;
import com.tinyspring.garderie.dto.EnfantResponseDTO;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.mapper.EnfantMapper;
import com.tinyspring.garderie.repository.EnfantRepository;
import com.tinyspring.garderie.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class EnfantServiceImpl implements EnfantService {

    private final EnfantRepository enfantRepository;
    private final UserRepository userRepository;
    private final EnfantMapper enfantMapper;

    public EnfantServiceImpl(EnfantRepository enfantRepository, UserRepository userRepository, EnfantMapper enfantMapper) {
        this.enfantRepository = enfantRepository;
        this.userRepository = userRepository;
        this.enfantMapper = enfantMapper;
    }

    @Override
    public Enfant ajouterEnfant(EnfantDTO dto) {
        User parent = userRepository.findById(dto.parentId)
                .orElseThrow(() -> new RuntimeException("Parent introuvable"));

        String nom = dto.nom != null ? dto.nom.trim() : "";
        String prenom = dto.prenom != null ? dto.prenom.trim() : "";
        String dateIso = dto.dateNaissance != null ? dto.dateNaissance.trim() : "";
        if (nom.isBlank() || prenom.isBlank() || dateIso.isBlank()) {
            throw new RuntimeException("Nom, prenom et date de naissance sont obligatoires.");
        }

        LocalDate dateNaissance = LocalDate.parse(dateIso);
        boolean exists = enfantRepository.existsByParentIdAndNomIgnoreCaseAndPrenomIgnoreCaseAndDateNaissanceAndArchiveFalse(
                parent.getId(), nom, prenom, dateNaissance);
        if (exists) {
            throw new RuntimeException("Cet enfant existe deja pour ce parent (meme nom, prenom et date de naissance).");
        }

        Enfant enfant = enfantMapper.toEntity(dto);
        enfant.setParent(parent);
        enfant.setArchive(false);

        return enfantRepository.save(enfant);
    }

    @Override
    public List<Enfant> getEnfantsParParent(Long parentId) {
        return enfantRepository.findByParentId(parentId);
    }

    @Override
    public Optional<Enfant> getEnfantParId(Long id) {
        return enfantRepository.findById(id);
    }

    @Override
    public Enfant modifierEnfant(Long id, EnfantDTO dto) {
        Enfant enfant = enfantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enfant introuvable"));

        User parent = userRepository.findById(dto.parentId)
                .orElseThrow(() -> new RuntimeException("Parent introuvable"));

        String nom = dto.nom != null ? dto.nom.trim() : "";
        String prenom = dto.prenom != null ? dto.prenom.trim() : "";
        String dateIso = dto.dateNaissance != null ? dto.dateNaissance.trim() : "";
        if (nom.isBlank() || prenom.isBlank() || dateIso.isBlank()) {
            throw new RuntimeException("Nom, prenom et date de naissance sont obligatoires.");
        }

        LocalDate dateNaissance = LocalDate.parse(dateIso);
        boolean exists = enfantRepository.existsByParentIdAndNomIgnoreCaseAndPrenomIgnoreCaseAndDateNaissanceAndArchiveFalseAndIdNot(
                parent.getId(), nom, prenom, dateNaissance, enfant.getId());
        if (exists) {
            throw new RuntimeException("Un enfant avec le meme nom, prenom et date de naissance existe deja pour ce parent.");
        }

        enfantMapper.updateEntityFromDto(dto, enfant);
        enfant.setParent(parent);

        return enfantRepository.save(enfant);
    }

    @Override
    public Enfant archiverEnfant(Long id) {
        Enfant enfant = enfantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enfant introuvable"));

        enfant.setArchive(true);
        return enfantRepository.save(enfant);
    }

    @Override
    public List<EnfantResponseDTO> getAllEnfants() {
        return enfantRepository.findByArchiveFalse()
                .stream()
                .map(enfantMapper::toResponseDto)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public EnfantResponseDTO getEnfantDTOById(Long id) {
        Enfant enfant = enfantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enfant introuvable"));
        return enfantMapper.toResponseDto(enfant);
    }
}
