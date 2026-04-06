package com.tinyspring.garderie.service.Events;

import com.tinyspring.garderie.dto.Events.DailyMenuResponse;
import com.tinyspring.garderie.dto.Events.DishRequest;
import com.tinyspring.garderie.dto.Events.WeeklyMenuRequest;
import com.tinyspring.garderie.dto.Events.WeeklyMenuResponse;
import com.tinyspring.garderie.entity.Events.DailyMenu;
import com.tinyspring.garderie.entity.Events.Dish;
import com.tinyspring.garderie.entity.Events.MenuStatus;
import com.tinyspring.garderie.entity.Events.WeeklyMenu;
import com.tinyspring.garderie.exception.Events.InvalidStatusTransitionException;
import com.tinyspring.garderie.exception.Events.ResourceNotFoundException;
import com.tinyspring.garderie.mappeer.WeeklyMenuMapper;
import com.tinyspring.garderie.repository.Events.DailyMenuRepository;
import com.tinyspring.garderie.repository.Events.DishRepository;
import com.tinyspring.garderie.repository.Events.WeeklyMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WeeklyMenuServiceImpl implements WeeklyMenuService {
    private final WeeklyMenuRepository weeklyMenuRepository;
    private final DailyMenuRepository dailyMenuRepository;
    private final DishRepository dishRepository;
    private final WeeklyMenuMapper weeklyMenuMapper;
    private final DailyMenuService dailyMenuService;
    private final DishService dishService;

    @Override
    @Transactional(readOnly = true)
    public List<WeeklyMenuResponse> getAll() {
        return weeklyMenuRepository.findAllByOrderByWeekStartDateDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WeeklyMenuResponse getById(Long id) {
        return toResponse(getEntity(id));
    }

    @Override
    public WeeklyMenuResponse create(WeeklyMenuRequest request) {
        WeeklyMenu weeklyMenu = weeklyMenuMapper.toEntity(request);
        applyDerivedDates(weeklyMenu);
        applyTemplateFlags(weeklyMenu);
        return toResponse(weeklyMenuRepository.save(weeklyMenu));
    }

    @Override
    public WeeklyMenuResponse update(Long id, WeeklyMenuRequest request) {
        WeeklyMenu weeklyMenu = getEntity(id);
        weeklyMenuMapper.updateEntityFromRequest(request, weeklyMenu);
        applyDerivedDates(weeklyMenu);
        applyTemplateFlags(weeklyMenu);
        return toResponse(weeklyMenuRepository.save(weeklyMenu));
    }

    @Override
    public void delete(Long id) {
        WeeklyMenu weeklyMenu = getEntity(id);
        List<DailyMenu> dailyMenus = dailyMenuRepository.findByWeeklyMenuIdOrderByMenuDate(weeklyMenu.getId());
        dailyMenus.forEach(dailyMenu -> dishRepository.findByDailyMenuId(dailyMenu.getId()).forEach(dishRepository::delete));
        dailyMenuRepository.deleteAll(dailyMenus);
        weeklyMenuRepository.delete(weeklyMenu);
    }

    @Override
    public WeeklyMenuResponse duplicate(Long id) {
        WeeklyMenu source = getEntity(id);

        if (source.getStatus() != MenuStatus.TEMPLATE) {
            throw new InvalidStatusTransitionException("Seul un menu template peut être dupliqué");
        }

        WeeklyMenu duplicate = WeeklyMenu.builder()
                .title(source.getTitle() + " (copie)")
                .weekStartDate(null)
                .weekEndDate(null)
                .status(MenuStatus.DRAFT)
                .isTemplate(false)
                .templateName(source.getTemplateName())
                .build();
        WeeklyMenu savedDuplicate = weeklyMenuRepository.save(duplicate);

        List<DailyMenu> sourceDailyMenus = dailyMenuRepository.findByWeeklyMenuIdOrderByMenuDate(source.getId());
        for (DailyMenu sourceDailyMenu : sourceDailyMenus) {
            DailyMenu copiedDailyMenu = DailyMenu.builder()
                    .weeklyMenuId(savedDuplicate.getId())
                    .menuDate(null)
                    .dayOfWeek(sourceDailyMenu.getDayOfWeek())
                    .isVisibleToParents(false)
                    .publishedAt(null)
                    .build();
            DailyMenu savedDailyMenu = dailyMenuRepository.save(copiedDailyMenu);

            List<Dish> sourceDishes = dishRepository.findByDailyMenuId(sourceDailyMenu.getId());
            for (Dish sourceDish : sourceDishes) {
                dishService.create(
                        DishRequest.builder()
                                .dailyMenuId(savedDailyMenu.getId())
                                .mealType(sourceDish.getMealType())
                                .name(sourceDish.getName())
                                .description(sourceDish.getDescription())
                                .photoUrl(sourceDish.getPhotoUrl())
                                .allergens(sourceDish.getAllergens())
                                .build()
                );
            }
        }

        return toResponse(savedDuplicate);
    }

    private WeeklyMenu getEntity(Long id) {
        return weeklyMenuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu hebdomadaire introuvable avec l'id : " + id));
    }

    private WeeklyMenuResponse toResponse(WeeklyMenu weeklyMenu) {
        WeeklyMenuResponse response = weeklyMenuMapper.toResponse(weeklyMenu);
        response.setIsTemplate(weeklyMenu.getStatus() == MenuStatus.TEMPLATE || weeklyMenu.isTemplate());
        response.setDailyMenus(dailyMenuService.getByWeeklyMenuId(weeklyMenu.getId()));
        return response;
    }

    private void applyTemplateFlags(WeeklyMenu weeklyMenu) {
        boolean template = weeklyMenu.getStatus() == MenuStatus.TEMPLATE || weeklyMenu.isTemplate();
        weeklyMenu.setTemplate(template);
        if (!template) {
            weeklyMenu.setTemplateName(null);
        }
    }

    private void applyDerivedDates(WeeklyMenu weeklyMenu) {
        LocalDate startDate = weeklyMenu.getWeekStartDate();
        if (startDate != null) {
            weeklyMenu.setWeekEndDate(startDate.plusDays(6));
        }
    }
}
