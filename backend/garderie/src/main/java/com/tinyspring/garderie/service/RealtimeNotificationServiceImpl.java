package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.RealtimeNotificationDto;
import com.tinyspring.garderie.entity.ObservationEnfant;
import com.tinyspring.garderie.repository.ObservationEnfantRepository;
import com.tinyspring.garderie.websocket.NotificationWebSocketHandler;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RealtimeNotificationServiceImpl implements RealtimeNotificationService {

    public static final String TYPE_PARENT_UNREAD_COUNT = "PARENT_UNREAD_OBSERVATIONS_COUNT";
    public static final String TYPE_ANIMATRICE_MEDICAMENT_DUE = "ANIMATRICE_MEDICAMENT_DUE";
    public static final String TYPE_PARENT_TRAITEMENT_VALIDATION = "PARENT_TRAITEMENT_VALIDATION";

    private final NotificationWebSocketHandler webSocketHandler;
    private final ObservationEnfantRepository observationRepository;

    public RealtimeNotificationServiceImpl(NotificationWebSocketHandler webSocketHandler,
                                       ObservationEnfantRepository observationRepository) {
        this.webSocketHandler = webSocketHandler;
        this.observationRepository = observationRepository;
    }

    public void notifyParentObservationCreated(String parentEmail, ObservationEnfant observation) {
        if (parentEmail == null || parentEmail.isBlank() || observation == null) {
            return;
        }

        long unread = observationRepository.countByEnfantParentEmailIgnoreCaseAndLuParentFalse(parentEmail.trim());

        RealtimeNotificationDto dto = new RealtimeNotificationDto();
        dto.setType(TYPE_PARENT_UNREAD_COUNT);
        dto.setUnreadObservationsCount(unread);
        if (observation.getEnfant() != null) {
            dto.setEnfantId(observation.getEnfant().getId());
        }
        dto.setObservationId(observation.getId());
        dto.setTitre(observation.getTitre());
        LocalDateTime creeLe = observation.getCreeLe();
        dto.setCreeLe(creeLe != null ? creeLe.toString() : null);

        webSocketHandler.sendToUser(parentEmail, dto);
    }

    public void notifyParentUnreadCountChanged(String parentEmail) {
        if (parentEmail == null || parentEmail.isBlank()) {
            return;
        }

        long unread = observationRepository.countByEnfantParentEmailIgnoreCaseAndLuParentFalse(parentEmail.trim());

        RealtimeNotificationDto dto = new RealtimeNotificationDto();
        dto.setType(TYPE_PARENT_UNREAD_COUNT);
        dto.setUnreadObservationsCount(unread);

        webSocketHandler.sendToUser(parentEmail, dto);
    }

    public void notifyAnimatricesDoseDue(Long enfantId,
                                         String enfantNom,
                                         String enfantPrenom,
                                         Long traitementId,
                                         String nomTraitement,
                                         String dateIso,
                                         String heurePrevue) {
        RealtimeNotificationDto dto = new RealtimeNotificationDto();
        dto.setType(TYPE_ANIMATRICE_MEDICAMENT_DUE);
        dto.setEnfantId(enfantId);
        dto.setEnfantNom(enfantNom);
        dto.setEnfantPrenom(enfantPrenom);
        dto.setTraitementId(traitementId);
        dto.setNomTraitement(nomTraitement);
        dto.setDatePrise(dateIso);
        dto.setHeurePrevue(heurePrevue);

        webSocketHandler.sendToRole("ROLE_ANIMATRICE", dto);
    }

    public void notifyParentTraitementValidation(String parentEmail,
                                                 Long enfantId,
                                                 Long traitementId,
                                                 String nomTraitement,
                                                 String statut,
                                                 String note) {
        if (parentEmail == null || parentEmail.isBlank()) {
            return;
        }

        RealtimeNotificationDto dto = new RealtimeNotificationDto();
        dto.setType(TYPE_PARENT_TRAITEMENT_VALIDATION);
        dto.setEnfantId(enfantId);
        dto.setTraitementId(traitementId);
        dto.setNomTraitement(nomTraitement);
        dto.setTitre(statut);
        dto.setCreeLe(null);
        dto.setHeurePrevue(null);
        dto.setDatePrise(null);
        dto.setValidationNote(note);

        webSocketHandler.sendToUser(parentEmail, dto);
    }
}
