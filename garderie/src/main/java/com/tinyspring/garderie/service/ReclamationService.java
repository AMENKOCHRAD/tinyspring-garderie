package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.RecommendedAdminActionResponse;
import com.tinyspring.garderie.dto.UpdateReclamationRequest;
import com.tinyspring.garderie.dto.UpdateReclamationStatusRequest;
import com.tinyspring.garderie.entity.Reclamation;
import com.tinyspring.garderie.entity.ReclamationHistory;
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

    List<ReclamationHistory> getReclamationHistory(Long reclamationId);

    byte[] exportReclamationHistoryPdf(Long reclamationId);

    byte[] exportReclamationsExcel();

    Reclamation updateReclamation(Long id, UpdateReclamationRequest request);

    Reclamation updateStatus(Long reclamationId, UpdateReclamationStatusRequest request);

    void deleteReclamation(Long id);

    String generateSuggestedAdminResponse(Long reclamationId);

    RecommendedAdminActionResponse getRecommendedAdminAction(Long reclamationId);
}