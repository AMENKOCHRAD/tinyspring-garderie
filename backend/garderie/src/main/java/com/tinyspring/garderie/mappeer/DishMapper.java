package com.tinyspring.garderie.mappeer;

import com.tinyspring.garderie.dto.Events.DishRequest;
import com.tinyspring.garderie.dto.Events.DishResponse;
import com.tinyspring.garderie.entity.Events.Dish;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DishMapper {
    @Mapping(target = "id", ignore = true)
    Dish toEntity(DishRequest request);

    DishResponse toResponse(Dish dish);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(DishRequest request, @MappingTarget Dish dish);
}
