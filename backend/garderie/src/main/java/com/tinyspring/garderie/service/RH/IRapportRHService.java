package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.RapportRHDTO;

import java.util.List;

public interface IRapportRHService {

    RapportRHDTO genererRapport(String question);

    List<RapportRHDTO> getTousLesRapports();

    RapportRHDTO getRapportById(Long id);
}
