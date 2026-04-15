package com.tinyspring.garderie.controller.RH;

import com.tinyspring.garderie.dto.RH.QuotaCongeDTO;
import com.tinyspring.garderie.entity.RH.QuotaConge;
import com.tinyspring.garderie.entity.RH.enums.TypeAbsenceConge;
import com.tinyspring.garderie.repository.RH.AbsenceCongeRepository;
import com.tinyspring.garderie.repository.RH.QuotaCongeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/quotas")
@RequiredArgsConstructor
public class QuotaCongeController {

    private final QuotaCongeRepository quotaCongeRepository;
    private final AbsenceCongeRepository absenceCongeRepository;

    @GetMapping
    public ResponseEntity<List<QuotaCongeDTO>> getAllQuotas() {
        List<QuotaCongeDTO> quotas = Arrays.stream(TypeAbsenceConge.values())
                .map(type -> {
                    QuotaConge quota = quotaCongeRepository.findByType(type)
                            .orElse(getQuotaParDefaut(type));

                    int annee = LocalDate.now().getYear();
                    int joursUtilises = absenceCongeRepository.findAll().stream()
                            .filter(a -> type.equals(a.getType()))
                            .filter(a -> a.getDateDebut().getYear() == annee)
                            .mapToInt(a -> a.getNbJours() != null ? a.getNbJours() : 0)
                            .sum();

                    return QuotaCongeDTO.builder()
                            .id(quota.getId())
                            .type(quota.getType())
                            .nbJoursMax(quota.getNbJoursMax())
                            .delaiPrevenanceJours(quota.getDelaiPrevenanceJours())
                            .effectifMinimum(quota.getEffectifMinimum())
                            .autoApprobation(quota.isAutoApprobation())
                            .joursUtilisesAnneeEnCours(joursUtilises)
                            .joursRestants(Math.max(0, quota.getNbJoursMax() - joursUtilises))
                            .build();
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(quotas);
    }

    @PutMapping("/{type}")
    public ResponseEntity<QuotaCongeDTO> updateQuota(
            @PathVariable TypeAbsenceConge type,
            @RequestBody QuotaCongeDTO dto) {

        QuotaConge quota = quotaCongeRepository.findByType(type)
                .orElse(QuotaConge.builder().type(type).build());

        quota.setNbJoursMax(dto.getNbJoursMax());
        quota.setDelaiPrevenanceJours(dto.getDelaiPrevenanceJours());
        quota.setEffectifMinimum(dto.getEffectifMinimum());
        quota.setAutoApprobation(dto.isAutoApprobation());

        QuotaConge saved = quotaCongeRepository.save(quota);

        return ResponseEntity.ok(QuotaCongeDTO.builder()
                .id(saved.getId())
                .type(saved.getType())
                .nbJoursMax(saved.getNbJoursMax())
                .delaiPrevenanceJours(saved.getDelaiPrevenanceJours())
                .effectifMinimum(saved.getEffectifMinimum())
                .autoApprobation(saved.isAutoApprobation())
                .build());
    }

    private QuotaConge getQuotaParDefaut(TypeAbsenceConge type) {
        return switch (type) {
            case CONGE_ANNUEL    -> QuotaConge.builder().type(type)
                    .nbJoursMax(30).delaiPrevenanceJours(7)
                    .effectifMinimum(2).autoApprobation(true).build();
            case CONGE_MALADIE   -> QuotaConge.builder().type(type)
                    .nbJoursMax(15).delaiPrevenanceJours(0)
                    .effectifMinimum(1).autoApprobation(true).build();
            case CONGE_MATERNITE -> QuotaConge.builder().type(type)
                    .nbJoursMax(90).delaiPrevenanceJours(30)
                    .effectifMinimum(1).autoApprobation(true).build();
            case ABSENCE         -> QuotaConge.builder().type(type)
                    .nbJoursMax(10).delaiPrevenanceJours(1)
                    .effectifMinimum(2).autoApprobation(false).build();
        };
    }
}