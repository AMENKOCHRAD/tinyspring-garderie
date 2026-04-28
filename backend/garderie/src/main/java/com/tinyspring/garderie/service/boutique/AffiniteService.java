package com.tinyspring.garderie.service.boutique;

import com.tinyspring.garderie.dto.boutique.InteractionRequestDto;
import com.tinyspring.garderie.dto.boutique.ProduitDto;
import com.tinyspring.garderie.dto.boutique.UserCategorieScoreAdminDto;
import com.tinyspring.garderie.entity.boutique.UserCategorieScore;
import java.util.List;

public interface AffiniteService {
    void enregistrerInteraction(Long userId, InteractionRequestDto dto);
    List<ProduitDto> getProduitRecommandes(Long userId);
    List<UserCategorieScore> getScoresUser(Long userId);
    List<UserCategorieScoreAdminDto> getAllScoresAdmin();
    List<UserCategorieScoreAdminDto> getScoresByUserAdmin(Long userId);
}