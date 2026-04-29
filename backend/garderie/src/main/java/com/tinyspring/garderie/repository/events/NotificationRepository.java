package com.tinyspring.garderie.repository.events;

import com.tinyspring.garderie.entity.events.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
        SELECT n FROM Notification n
        WHERE n.parent.id = :parentId
        ORDER BY
            CASE WHEN n.seen = false THEN 0 ELSE 1 END,
            CASE WHEN n.priority = 'URGENT' THEN 0 ELSE 1 END,
            n.createdAt DESC
    """)
    List<Notification> findByParentOrdered(@Param("parentId") Long parentId);

    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE n.parent.id = :parentId
        AND n.seen = false
    """)
    long countUnreadByParentId(@Param("parentId") Long parentId);

    boolean existsByParent_IdAndType(Long parentId, String type);

    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.seen = true
        WHERE n.parent.id = :parentId
        AND n.seen = false
    """)
    void markAllReadByParentId(@Param("parentId") Long parentId);
}