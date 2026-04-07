package com.tinyspring.garderie.service.Events;

import com.tinyspring.garderie.dto.Events.DailyMenuRequest;
import com.tinyspring.garderie.dto.Events.DailyMenuResponse;
import com.tinyspring.garderie.entity.Events.DailyMenu;
import com.tinyspring.garderie.entity.Events.WeeklyMenu;
import com.tinyspring.garderie.exception.Events.ResourceNotFoundException;
import com.tinyspring.garderie.mappeer.DailyMenuMapper;
import com.tinyspring.garderie.repository.Events.DailyMenuRepository;
import com.tinyspring.garderie.repository.Events.WeeklyMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DailyMenuServiceImpl implements DailyMenuService {

    private final DailyMenuRepository dailyMenuRepository;
    private final WeeklyMenuRepository weeklyMenuRepository;
    private final DailyMenuMapper dailyMenuMapper;

    @Override
    public DailyMenuResponse create(DailyMenuRequest request) {
        if (request.getWeeklyMenuId() == null) {
            throw new IllegalArgumentException("weeklyMenuId est obligatoire pour la création standalone d'un jour");
        }

        WeeklyMenu weeklyMenu = getWeeklyMenuEntity(request.getWeeklyMenuId());
        DailyMenu dailyMenu = dailyMenuMapper.toEntity(request);
        dailyMenu.setWeeklyMenu(weeklyMenu);
        dailyMenu.setVisibleToParents(Boolean.TRUE.equals(request.getIsVisibleToParents()));

        return dailyMenuMapper.toResponse(dailyMenuRepository.save(dailyMenu));
    }

    @Override
    public DailyMenuResponse update(Long id, DailyMenuRequest request) {
        DailyMenu dailyMenu = getEntity(id);

        dailyMenuMapper.updateEntityFromRequest(request, dailyMenu);

        if (request.getWeeklyMenuId() != null) {
            WeeklyMenu weeklyMenu = getWeeklyMenuEntity(request.getWeeklyMenuId());
            dailyMenu.setWeeklyMenu(weeklyMenu);
        }

        if (request.getIsVisibleToParents() != null) {
            dailyMenu.setVisibleToParents(request.getIsVisibleToParents());
        }

        return dailyMenuMapper.toResponse(dailyMenuRepository.save(dailyMenu));
    }

    @Override
    public void delete(Long id) {
        DailyMenu dailyMenu = getEntity(id);
        dailyMenuRepository.delete(dailyMenu);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyMenuResponse> getByWeeklyMenuId(Long weeklyMenuId) {
        return dailyMenuRepository.findByWeeklyMenu_IdOrderByMenuDateAsc(weeklyMenuId)
                .stream()
                .map(dailyMenuMapper::toResponse)
                .toList();
    }

    private DailyMenu getEntity(Long id) {
        return dailyMenuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu journalier introuvable avec l'id : " + id));
    }

    private WeeklyMenu getWeeklyMenuEntity(Long weeklyMenuId) {
        return weeklyMenuRepository.findById(weeklyMenuId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu hebdomadaire introuvable avec l'id : " + weeklyMenuId));
    }
}