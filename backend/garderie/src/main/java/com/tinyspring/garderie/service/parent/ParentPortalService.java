package com.tinyspring.garderie.service.parent;

import com.tinyspring.garderie.dto.Events.*;
import com.tinyspring.garderie.dto.Parent.ParentChildResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ParentPortalService {
    List<ParentChildResponse> getChildren(Long parentId);

    List<EventResponse> getEvents(Long parentId);

    List<EventRegistrationResponse> getParticipations(Long parentId);

    EventRegistrationResponse participate(Long parentId,
                                          Long eventId,
                                          Long childId,
                                          String notes,
                                          MultipartFile authorizationFile);

    EventRegistrationResponse cancelParticipation(Long parentId, Long registrationId);
    EventRatingResponse rateEvent(Long parentId, Long eventId, EventRatingRequest request);
    List<WeeklyMenuResponse> getMenus(Long parentId);
}
