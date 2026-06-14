package com.devteam.aiauditserver.repositories;

import com.devteam.aiauditserver.models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientUserIdAndReadFalse(Long userId);

    /** Shared admin feed — newest first. */
    List<Notification> findAllByOrderByCreatedAtDesc();

    long countByReadFalse();

    /** Idempotency guard so a given audit only ever yields one notification. */
    boolean existsByAuditRequestIdAndType(Long auditRequestId, String type);

    @Modifying
    @Query("update Notification n set n.read = true where n.id in :ids")
    void markReadByIds(@Param("ids") List<Long> ids);

    @Modifying
    @Query("update Notification n set n.read = true where n.read = false")
    void markAllRead();
}
