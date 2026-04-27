package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.AnimatriceDTO;
import com.tinyspring.garderie.entity.RH.Animatrice;
import com.tinyspring.garderie.entity.RH.enums.StatutAnimatrice;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IAnimatriceService {

    List<AnimatriceDTO> getAllAnimatrices();

    AnimatriceDTO getAnimatriceById(Long id);

    AnimatriceDTO getAnimatriceByEmail(String email);

    AnimatriceDTO createAnimatrice(AnimatriceDTO dto);

    AnimatriceDTO updateAnimatrice(Long id, AnimatriceDTO dto);

    AnimatriceDTO updateMonProfil(Long id, AnimatriceDTO dto);

    void deleteAnimatrice(Long id);

    List<AnimatriceDTO> getAnimatricesByStatut(StatutAnimatrice statut);

    AnimatriceDTO uploadPhoto(Long id, MultipartFile file) throws IOException;

    // ✅ Exposé pour DashboardServiceImpl
    AnimatriceDTO toDTO(Animatrice animatrice);
}
