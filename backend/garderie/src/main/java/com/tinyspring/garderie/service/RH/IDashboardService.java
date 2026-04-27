package com.tinyspring.garderie.service.RH;

import com.tinyspring.garderie.dto.RH.AbsenceCongeDTO;
import com.tinyspring.garderie.dto.RH.DashboardStatsDTO;
import com.tinyspring.garderie.entity.RH.AbsenceConge;

public interface IDashboardService {

    DashboardStatsDTO getStats();

    AbsenceCongeDTO toDTO(AbsenceConge absenceConge);
}
