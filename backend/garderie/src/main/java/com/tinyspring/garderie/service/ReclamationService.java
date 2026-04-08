package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.UpdateReclamationRequest;
import com.tinyspring.garderie.dto.UpdateReclamationStatusRequest;
import com.tinyspring.garderie.entity.Reclamation;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ReclamationService {

    Reclamation createReclamation(String title,
                                  String description,
                                  String priority,
                                  String category,
                                  MultipartFile image,
                                  MultipartFile attachment);

    List<Reclamation> getMyReclamations();

    Reclamation getReclamationById(Long id);

    Reclamation updateReclamation(Long id, UpdateReclamationRequest request);

    Reclamation updateStatus(Long reclamationId, UpdateReclamationStatusRequest request);

    void deleteReclamation(Long id);
}