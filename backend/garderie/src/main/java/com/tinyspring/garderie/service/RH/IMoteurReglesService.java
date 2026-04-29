package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.ResultatEvaluationDTO;
import com.tinyspring.garderie.entity.RH.AbsenceConge;

public interface IMoteurReglesService {

    ResultatEvaluationDTO evaluer(AbsenceConge demande);
}
