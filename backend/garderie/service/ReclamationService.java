package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.CreateReclamationRequest;
import com.tinyspring.garderie.dto.UpdateReclamationRequest;
import com.tinyspring.garderie.dto.UpdateReclamationStatusRequest;
import com.tinyspring.garderie.entity.Reclamation;

import java.util.List;

public interface ReclamationService {

    Reclamation createReclamation(CreateReclamationRequest request);

    List<Reclamation> getMyReclamations();

    Reclamation getReclamationById(Long id);

    Reclamation updateReclamation(Long id, UpdateReclamationRequest request);

    Reclamation updateStatus(Long reclamationId, UpdateReclamationStatusRequest request);

    void deleteReclamation(Long id);
}