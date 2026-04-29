package com.tinyspring.garderie.mappeer;

import com.tinyspring.garderie.dto.Events.DishRequest;
import com.tinyspring.garderie.dto.Events.DishResponse;
import com.tinyspring.garderie.entity.events.Dish;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DishMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dailyMenu", ignore = true)
    Dish toEntity(DishRequest request);

    @Mapping(target = "dailyMenuId", source = "dailyMenu.id")
    DishResponse toResponse(Dish dish);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dailyMenu", ignore = true)
    void updateEntityFromRequest(DishRequest request, @MappingTarget Dish dish);
}