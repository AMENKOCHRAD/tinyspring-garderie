package com.tinyspring.garderie.mappeer;

import com.tinyspring.garderie.dto.Events.DailyMenuRequest;
import com.tinyspring.garderie.dto.Events.DailyMenuResponse;
import com.tinyspring.garderie.entity.events.DailyMenu;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = DishMapper.class, builder = @Builder(disableBuilder = true))
public interface DailyMenuMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "visibleToParents", source = "isVisibleToParents")
    @Mapping(target = "weeklyMenu", ignore = true)
    @Mapping(target = "dishes", ignore = true)
    DailyMenu toEntity(DailyMenuRequest request);

    @Mapping(target = "weeklyMenuId", source = "weeklyMenu.id")
    @Mapping(target = "isVisibleToParents", expression = "java(dailyMenu.isVisibleToParents())")
    DailyMenuResponse toResponse(DailyMenu dailyMenu);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "visibleToParents", source = "isVisibleToParents")
    @Mapping(target = "weeklyMenu", ignore = true)
    @Mapping(target = "dishes", ignore = true)
    void updateEntityFromRequest(DailyMenuRequest request, @MappingTarget DailyMenu dailyMenu);
}
