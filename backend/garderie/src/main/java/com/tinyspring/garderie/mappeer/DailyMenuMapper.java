package com.tinyspring.garderie.mappeer;

import com.tinyspring.garderie.dto.Events.DailyMenuRequest;
import com.tinyspring.garderie.dto.Events.DailyMenuResponse;
import com.tinyspring.garderie.entity.Events.DailyMenu;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = DishMapper.class)
public interface DailyMenuMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "weeklyMenu", ignore = true)
    @Mapping(target = "dishes", ignore = true)
    DailyMenu toEntity(DailyMenuRequest request);

    @Mapping(target = "weeklyMenuId", source = "weeklyMenu.id")
    DailyMenuResponse toResponse(DailyMenu dailyMenu);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "weeklyMenu", ignore = true)
    @Mapping(target = "dishes", ignore = true)
    void updateEntityFromRequest(DailyMenuRequest request, @MappingTarget DailyMenu dailyMenu);
}