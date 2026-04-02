package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.AnimatriceDTO;
import com.tinyspring.garderie.dto.RH.FormationDTO;
import com.tinyspring.garderie.entity.RH.Animatrice;
import com.tinyspring.garderie.entity.RH.Formation;
import com.tinyspring.garderie.entity.RH.enums.StatutFormation;
import com.tinyspring.garderie.repository.RH.AnimatriceRepository;
import com.tinyspring.garderie.repository.RH.FormationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormationService {

    private final FormationRepository formationRepository;
    private final AnimatriceRepository animatriceRepository;
    private final AnimatriceService animatriceService;

    // ========== ADMIN ==========

    public List<FormationDTO> getAllFormations() {
        return formationRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public FormationDTO getFormationById(Long id) {
        Formation formation = formationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Formation non trouvée avec l'id : " + id));
        return toDTO(formation);
    }

    public FormationDTO createFormation(FormationDTO dto) {
        Formation formation = toEntity(dto);
        formation.setStatutInscription(StatutFormation.INSCRITE);
        return toDTO(formationRepository.save(formation));
    }

    public FormationDTO updateFormation(Long id, FormationDTO dto) {
        Formation formation = formationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Formation non trouvée avec l'id : " + id));
        formation.setTitre(dto.getTitre());
        formation.setDescription(dto.getDescription());
        formation.setType(dto.getType());
        formation.setDateDebut(dto.getDateDebut());
        formation.setDateFin(dto.getDateFin());
        formation.setFormateur(dto.getFormateur());
        formation.setPlacesMax(dto.getPlacesMax());
        formation.setStatutInscription(dto.getStatutInscription());
        return toDTO(formationRepository.save(formation));
    }

    public void deleteFormation(Long id) {
        if (!formationRepository.existsById(id)) {
            throw new EntityNotFoundException("Formation non trouvée avec l'id : " + id);
        }
        formationRepository.deleteById(id);
    }

    public FormationDTO updateStatutFormation(Long id, StatutFormation statut) {
        Formation formation = formationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Formation non trouvée"));
        formation.setStatutInscription(statut);
        return toDTO(formationRepository.save(formation));
    }

    // ========== ANIMATRICE ==========

    public FormationDTO sInscrireFormation(Long formationId, Long animatriceId) {
        Formation formation = formationRepository.findById(formationId)
                .orElseThrow(() -> new EntityNotFoundException("Formation non trouvée"));
        Animatrice animatrice = animatriceRepository.findById(animatriceId)
                .orElseThrow(() -> new EntityNotFoundException("Animatrice non trouvée"));

        if (formation.getAnimatrices().size() >= formation.getPlacesMax()) {
            throw new RuntimeException("Plus de places disponibles pour cette formation");
        }
        if (formation.getAnimatrices().contains(animatrice)) {
            throw new RuntimeException("Vous êtes déjà inscrite à cette formation");
        }

        formation.getAnimatrices().add(animatrice);
        return toDTO(formationRepository.save(formation));
    }

    public List<FormationDTO> getMesFormations(Long animatriceId) {
        return formationRepository.findByAnimatricesId(animatriceId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<FormationDTO> getFormationsDisponibles() {
        return formationRepository.findByStatutInscription(StatutFormation.INSCRITE)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ========== MAPPING ==========

    private FormationDTO toDTO(Formation formation) {
        List<AnimatriceDTO> animatricesDTO = formation.getAnimatrices() == null ? List.of() :
                formation.getAnimatrices().stream()
                        .map(animatriceService::toDTO)
                        .collect(Collectors.toList());
        return FormationDTO.builder()
                .id(formation.getId())
                .titre(formation.getTitre())
                .description(formation.getDescription())
                .type(formation.getType())
                .dateDebut(formation.getDateDebut())
                .dateFin(formation.getDateFin())
                .formateur(formation.getFormateur())
                .placesMax(formation.getPlacesMax())
                .statutInscription(formation.getStatutInscription())
                .animatrices(animatricesDTO)
                .build();
    }

    private Formation toEntity(FormationDTO dto) {
        return Formation.builder()
                .titre(dto.getTitre())
                .description(dto.getDescription())
                .type(dto.getType())
                .dateDebut(dto.getDateDebut())
                .dateFin(dto.getDateFin())
                .formateur(dto.getFormateur())
                .placesMax(dto.getPlacesMax())
                .build();
    }
}