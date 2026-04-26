package com.tinyspring.garderie.repository.Events;

import com.tinyspring.garderie.entity.Events.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByParentIdOrderByCreatedAtDesc(Long parentId);

    boolean existsByParentIdAndType(Long parentId, String type);
}
