package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.EnfantDTO;
import com.tinyspring.garderie.dto.EnfantResponseDTO;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.User;
import com.tinyspring.garderie.repository.EnfantRepository;
import com.tinyspring.garderie.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class EnfantService {

    private final EnfantRepository enfantRepository;
    private final UserRepository userRepository;

    public EnfantService(EnfantRepository enfantRepository, UserRepository userRepository) {
        this.enfantRepository = enfantRepository;
        this.userRepository = userRepository;
    }

    public Enfant ajouterEnfant(EnfantDTO dto) {
        User parent = userRepository.findById(dto.parentId)
                .orElseThrow(() -> new RuntimeException("Parent introuvable"));

        Enfant enfant = new Enfant();
        enfant.setNom(dto.nom);
        enfant.setPrenom(dto.prenom);
        enfant.setDateNaissance(LocalDate.parse(dto.dateNaissance));
        enfant.setGroupeSanguin(dto.groupeSanguin);
        enfant.setAllergies(dto.allergies);
        enfant.setContactUrgence(dto.contactUrgence);
        enfant.setPhoto(dto.photo);
        enfant.setParent(parent);
        enfant.setArchive(false);

        return enfantRepository.save(enfant);
    }

    public List<Enfant> getEnfantsParParent(Long parentId) {
        return enfantRepository.findByParentId(parentId);
    }

    public Optional<Enfant> getEnfantParId(Long id) {
        return enfantRepository.findById(id);
    }

    public Enfant modifierEnfant(Long id, EnfantDTO dto) {
        Enfant enfant = enfantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enfant introuvable"));

        User parent = userRepository.findById(dto.parentId)
                .orElseThrow(() -> new RuntimeException("Parent introuvable"));

        enfant.setNom(dto.nom);
        enfant.setPrenom(dto.prenom);
        enfant.setDateNaissance(LocalDate.parse(dto.dateNaissance));
        enfant.setGroupeSanguin(dto.groupeSanguin);
        enfant.setAllergies(dto.allergies);
        enfant.setContactUrgence(dto.contactUrgence);
        enfant.setPhoto(dto.photo);
        enfant.setParent(parent);

        return enfantRepository.save(enfant);
    }

    public Enfant archiverEnfant(Long id) {
        Enfant enfant = enfantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enfant introuvable"));

        enfant.setArchive(true);
        return enfantRepository.save(enfant);
    }

    private EnfantResponseDTO toDTO(Enfant enfant) {
        EnfantResponseDTO dto = new EnfantResponseDTO();
        dto.id = enfant.getId();
        dto.nom = enfant.getNom();
        dto.prenom = enfant.getPrenom();
        dto.dateNaissance = enfant.getDateNaissance().toString();
        dto.groupeSanguin = enfant.getGroupeSanguin();
        dto.allergies = enfant.getAllergies();
        dto.contactUrgence = enfant.getContactUrgence();
        dto.photo = enfant.getPhoto();

        EnfantResponseDTO.ParentDTO parentDTO = new EnfantResponseDTO.ParentDTO();
        parentDTO.id = enfant.getParent().getId();
        parentDTO.nom = enfant.getParent().getNom();
        parentDTO.email = enfant.getParent().getEmail();

        dto.parent = parentDTO;

        return dto;
    }

    public List<EnfantResponseDTO> getAllEnfants() {
        return enfantRepository.findByArchiveFalse()
                .stream()
                .map(this::toDTO)
                .collect(java.util.stream.Collectors.toList());
    }
 
    public EnfantResponseDTO getEnfantDTOById(Long id) {
        Enfant enfant = enfantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Enfant introuvable"));
        return toDTO(enfant);
    }
}