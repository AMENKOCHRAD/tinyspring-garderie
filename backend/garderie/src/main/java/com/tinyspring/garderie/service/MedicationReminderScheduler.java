package com.tinyspring.garderie.service;

import com.tinyspring.garderie.entity.ConditionSanitaire;
import com.tinyspring.garderie.entity.Enfant;
import com.tinyspring.garderie.entity.StatutTraitement;
import com.tinyspring.garderie.entity.Traitement;
import com.tinyspring.garderie.repository.PriseTraitementRepository;
import com.tinyspring.garderie.repository.TraitementRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class MedicationReminderScheduler {

    private final TraitementRepository traitementRepository;
    private final PriseTraitementRepository priseTraitementRepository;
    private final RealtimeNotificationService realtimeNotificationService;

    public MedicationReminderScheduler(TraitementRepository traitementRepository,
                                       PriseTraitementRepository priseTraitementRepository,
                                       RealtimeNotificationService realtimeNotificationService) {
        this.traitementRepository = traitementRepository;
        this.priseTraitementRepository = priseTraitementRepository;
        this.realtimeNotificationService = realtimeNotificationService;
    }

    @Scheduled(cron = "0 * * * * *") // every minute
    @Transactional(readOnly = true)
    public void tick() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalTime current = now.toLocalTime().withSecond(0).withNano(0);

        DayOfWeek dow = today.getDayOfWeek();
        if (dow == DayOfWeek.SUNDAY) {
            return;
        }
        if (dow == DayOfWeek.SATURDAY && current.isAfter(LocalTime.of(12, 30))) {
            return;
        }

        List<Traitement> traitements = traitementRepository.findActifsPourDate(
                List.of(StatutTraitement.VALIDE, StatutTraitement.ACTIF),
                today
        );

        for (Traitement traitement : traitements) {
            if (traitement == null || traitement.getHeuresPrises() == null || traitement.getHeuresPrises().isEmpty()) {
                continue;
            }

            Optional<LocalTime> dueTime = findDueTime(traitement.getHeuresPrises(), current);
            if (dueTime.isEmpty()) {
                continue;
            }

            String heurePrevue = formatHeure(current);
            // Use original heure string when it matches current time
            String heureStored = findMatchingHeureString(traitement.getHeuresPrises(), current).orElse(heurePrevue);

            boolean dejaDonne = priseTraitementRepository
                    .findByTraitementIdAndDatePriseAndHeurePrevue(traitement.getId(), today, heureStored)
                    .isPresent();
            if (dejaDonne) {
                continue;
            }

            ConditionSanitaire condition = traitement.getConditionSanitaire();
            Enfant enfant = condition != null ? condition.getEnfant() : null;

            realtimeNotificationService.notifyAnimatricesDoseDue(
                    enfant != null ? enfant.getId() : null,
                    enfant != null ? enfant.getNom() : null,
                    enfant != null ? enfant.getPrenom() : null,
                    traitement.getId(),
                    traitement.getNomTraitement(),
                    today.toString(),
                    heureStored
            );
        }
    }

    private static Optional<LocalTime> findDueTime(List<String> heures, LocalTime current) {
        for (String h : heures) {
            LocalTime parsed = parseHeure(h);
            if (parsed != null && parsed.getHour() == current.getHour() && parsed.getMinute() == current.getMinute()) {
                return Optional.of(parsed);
            }
        }
        return Optional.empty();
    }

    private static Optional<String> findMatchingHeureString(List<String> heures, LocalTime current) {
        for (String h : heures) {
            LocalTime parsed = parseHeure(h);
            if (parsed != null && parsed.getHour() == current.getHour() && parsed.getMinute() == current.getMinute()) {
                return Optional.of(h);
            }
        }
        return Optional.empty();
    }

    private static LocalTime parseHeure(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            return null;
        }

        // Supports "09:00", "9:00", "9h", "9h30", "9"
        try {
            if (value.contains("h")) {
                String[] parts = value.split("h", 2);
                int hour = Integer.parseInt(parts[0].trim());
                int minute = 0;
                if (parts.length > 1 && !parts[1].trim().isBlank()) {
                    minute = Integer.parseInt(parts[1].trim());
                }
                if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return null;
                return LocalTime.of(hour, minute);
            }

            if (value.contains(":")) {
                String[] parts = value.split(":", 2);
                int hour = Integer.parseInt(parts[0].trim());
                int minute = Integer.parseInt(parts[1].trim());
                if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return null;
                return LocalTime.of(hour, minute);
            }

            int hour = Integer.parseInt(value);
            if (hour < 0 || hour > 23) return null;
            return LocalTime.of(hour, 0);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String formatHeure(LocalTime time) {
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }
}
