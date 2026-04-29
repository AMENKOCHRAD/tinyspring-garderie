package com.tinyspring.garderie.service.events;

import com.tinyspring.garderie.dto.Events.DailyMenuRequest;
import com.tinyspring.garderie.dto.Events.DishRequest;
import com.tinyspring.garderie.dto.Events.WeeklyMenuRequest;
import com.tinyspring.garderie.dto.Events.WeeklyMenuResponse;
import com.tinyspring.garderie.entity.events.DailyMenu;
import com.tinyspring.garderie.entity.events.Dish;
import com.tinyspring.garderie.entity.events.MenuStatus;
import com.tinyspring.garderie.entity.events.WeeklyMenu;
import com.tinyspring.garderie.exception.Events.InvalidStatusTransitionException;
import com.tinyspring.garderie.exception.Events.ResourceNotFoundException;
import com.tinyspring.garderie.mapper.DailyMenuMapper;
import com.tinyspring.garderie.mapper.DishMapper;
import com.tinyspring.garderie.mapper.WeeklyMenuMapper;
import com.tinyspring.garderie.repository.events.WeeklyMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WeeklyMenuServiceImpl implements WeeklyMenuService {

    private final WeeklyMenuRepository weeklyMenuRepository;
    private final WeeklyMenuMapper weeklyMenuMapper;
    private final DailyMenuMapper dailyMenuMapper;
    private final DishMapper dishMapper;

    @Override
    @Transactional(readOnly = true)
    public List<WeeklyMenuResponse> getAll() {
        return weeklyMenuRepository.findAllByOrderByWeekStartDateDesc()
                .stream()
                .map(weeklyMenuMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WeeklyMenuResponse getById(Long id) {
        return weeklyMenuMapper.toResponse(getEntity(id));
    }

    @Override
    public WeeklyMenuResponse create(WeeklyMenuRequest request) {
        WeeklyMenu weeklyMenu = weeklyMenuMapper.toEntity(request);

        applyDerivedDates(weeklyMenu);
        applyDefaultStatus(weeklyMenu);
        applyTemplateFlags(weeklyMenu);

        buildDailyMenusGraph(weeklyMenu, request);

        WeeklyMenu saved = weeklyMenuRepository.save(weeklyMenu);
        return weeklyMenuMapper.toResponse(saved);
    }

    @Override
    public WeeklyMenuResponse update(Long id, WeeklyMenuRequest request) {
        WeeklyMenu weeklyMenu = getEntity(id);

        weeklyMenuMapper.updateEntityFromRequest(request, weeklyMenu);
        applyDerivedDates(weeklyMenu);
        applyDefaultStatus(weeklyMenu);
        applyTemplateFlags(weeklyMenu);

        if (request.getDailyMenus() != null) {
            weeklyMenu.getDailyMenus().clear();
            buildDailyMenusGraph(weeklyMenu, request);
        }

        WeeklyMenu saved = weeklyMenuRepository.save(weeklyMenu);
        return weeklyMenuMapper.toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        WeeklyMenu weeklyMenu = getEntity(id);
        weeklyMenuRepository.delete(weeklyMenu);
    }

    @Override
    public WeeklyMenuResponse duplicate(Long id) {
        WeeklyMenu source = getEntity(id);

        if (!Boolean.TRUE.equals(source.isTemplate()) && source.getStatus() != MenuStatus.PUBLISHED) {
            throw new InvalidStatusTransitionException("Seul un menu template peut être dupliqué");
        }

        WeeklyMenu duplicate = WeeklyMenu.builder()
                .title(source.getTitle() + " (copie)")
                .weekStartDate(null)
                .weekEndDate(null)
                .status(MenuStatus.DRAFT)
                .isTemplate(false)
                .templateName(null)
                .build();

        for (DailyMenu sourceDailyMenu : source.getDailyMenus()) {
            DailyMenu copiedDailyMenu = DailyMenu.builder()
                    .menuDate(null)
                    .dayOfWeek(sourceDailyMenu.getDayOfWeek())
                    .isVisibleToParents(sourceDailyMenu.isVisibleToParents())
                    .publishedAt(null)
                    .build();

            for (Dish sourceDish : sourceDailyMenu.getDishes()) {
                Dish copiedDish = Dish.builder()
                        .mealType(sourceDish.getMealType())
                        .name(sourceDish.getName())
                        .description(sourceDish.getDescription())
                        .allergens(sourceDish.getAllergens())
                        .allergenConflictFlags(sourceDish.getAllergenConflictFlags())
                        .build();

                copiedDailyMenu.addDish(copiedDish);
            }

            duplicate.addDailyMenu(copiedDailyMenu);
        }

        WeeklyMenu saved = weeklyMenuRepository.save(duplicate);
        return weeklyMenuMapper.toResponse(saved);
    }

    private WeeklyMenu getEntity(Long id) {
        return weeklyMenuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu hebdomadaire introuvable avec l'id : " + id));
    }

    private void applyDefaultStatus(WeeklyMenu weeklyMenu) {
        if (weeklyMenu.getStatus() == null) {
            weeklyMenu.setStatus(MenuStatus.DRAFT);
        }
    }

    private void applyTemplateFlags(WeeklyMenu weeklyMenu) {
        boolean template = weeklyMenu.getStatus() == MenuStatus.TEMPLATE || weeklyMenu.isTemplate();
        weeklyMenu.setTemplate(template);
        if (weeklyMenu.getStatus() == MenuStatus.TEMPLATE) {
            weeklyMenu.setStatus(MenuStatus.DRAFT);
        }

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

    private void buildDailyMenusGraph(WeeklyMenu weeklyMenu, WeeklyMenuRequest request) {
        List<DailyMenuRequest> dailyMenuRequests = request.getDailyMenus();

        if (dailyMenuRequests == null || dailyMenuRequests.isEmpty()) {
            return;
        }

        List<DailyMenuRequest> sortedDays = dailyMenuRequests.stream()
                .sorted(Comparator.comparing(
                        d -> d.getMenuDate() != null ? d.getMenuDate() : LocalDate.MAX
                ))
                .toList();

        for (int i = 0; i < sortedDays.size(); i++) {
            DailyMenuRequest dailyRequest = sortedDays.get(i);

            DailyMenu dailyMenu = dailyMenuMapper.toEntity(dailyRequest);

            LocalDate computedDate = dailyRequest.getMenuDate() != null
                    ? dailyRequest.getMenuDate()
                    : weeklyMenu.getWeekStartDate().plusDays(i);

            dailyMenu.setMenuDate(computedDate);
            dailyMenu.setDayOfWeek(
                    dailyRequest.getDayOfWeek() != null
                            ? dailyRequest.getDayOfWeek()
                            : computedDate.getDayOfWeek()
            );
            dailyMenu.setVisibleToParents(Boolean.TRUE.equals(dailyRequest.getIsVisibleToParents()));

            weeklyMenu.addDailyMenu(dailyMenu);

            if (dailyRequest.getDishes() != null) {
                for (DishRequest dishRequest : dailyRequest.getDishes()) {
                    Dish dish = dishMapper.toEntity(dishRequest);
                    dailyMenu.addDish(dish);
                }
            }
        }
    }

}
