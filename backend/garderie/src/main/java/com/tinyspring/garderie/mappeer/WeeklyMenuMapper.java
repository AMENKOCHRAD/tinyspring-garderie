package com.tinyspring.garderie.mappeer;

import com.tinyspring.garderie.dto.Events.WeeklyMenuRequest;
import com.tinyspring.garderie.dto.Events.WeeklyMenuResponse;
import com.tinyspring.garderie.entity.Events.WeeklyMenu;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = DailyMenuMapper.class)
public interface WeeklyMenuMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "dailyMenus", ignore = true)
    WeeklyMenu toEntity(WeeklyMenuRequest request);

    WeeklyMenuResponse toResponse(WeeklyMenu weeklyMenu);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "dailyMenus", ignore = true)
    void updateEntityFromRequest(WeeklyMenuRequest request, @MappingTarget WeeklyMenu weeklyMenu);
}