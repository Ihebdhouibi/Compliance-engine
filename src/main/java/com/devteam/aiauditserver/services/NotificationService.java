package com.devteam.aiauditserver.services;

import com.devteam.aiauditserver.models.Notification;
import com.devteam.aiauditserver.repositories.NotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;

    public NotificationService(SimpMessagingTemplate messagingTemplate,
                               NotificationRepository notificationRepository) {
        this.messagingTemplate = messagingTemplate;
        this.notificationRepository = notificationRepository;
    }

    public Notification createAndSend(String message, String type, Long recipientUserId, Long auditRequestId) {
        Notification notif = new Notification(message, type, recipientUserId, auditRequestId);
        notificationRepository.save(notif);
        messagingTemplate.convertAndSendToUser(recipientUserId.toString(), "/queue/notifications", notif);
        return notif;
    }
}