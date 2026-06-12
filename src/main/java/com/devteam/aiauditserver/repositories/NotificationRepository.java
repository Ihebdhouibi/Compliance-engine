package com.devteam.aiauditserver.repositories;

import com.devteam.aiauditserver.models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientUserIdAndReadFalse(Long userId);
}
