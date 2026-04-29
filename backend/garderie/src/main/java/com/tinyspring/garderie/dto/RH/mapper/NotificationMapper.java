package com.tinyspring.garderie.dto.RH.mapper;

import com.tinyspring.garderie.dto.RH.NotificationDTO;
import com.tinyspring.garderie.entity.RH.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationDTO toDTO(Notification notification);
}