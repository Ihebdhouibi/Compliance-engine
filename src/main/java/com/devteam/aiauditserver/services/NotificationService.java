package com.devteam.aiauditserver.services;

import com.devteam.aiauditserver.models.Notification;
import com.devteam.aiauditserver.models.User.User;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequest;
import com.devteam.aiauditserver.repositories.NotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
public class NotificationService {

    /** Notification raised when a company user submits an audit request. */
    public static final String TYPE_AUDIT_SUBMITTED = "AUDIT_SUBMITTED";

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

    // ── shared admin feed ───────────────────────────────────────────────

    public List<Notification> listAll() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    public long unreadCount() {
        return notificationRepository.countByReadFalse();
    }

    @Transactional
    public void markRead(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        notificationRepository.markReadByIds(ids);
    }

    @Transactional
    public void markAllRead() {
        notificationRepository.markAllRead();
    }

    @Transactional
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        notificationRepository.deleteAllById(ids);
    }

    /**
     * Records the "New audit request from X" notification for the shared admin
     * feed. Idempotent per audit request, so re-submits or a backfill never
     * duplicate it. recipientUserId is null = broadcast to all admins.
     */
    public Notification notifyAuditSubmitted(AuditRequest req) {
        if (req == null || req.getId() == null) return null;
        if (notificationRepository.existsByAuditRequestIdAndType(req.getId(), TYPE_AUDIT_SUBMITTED)) {
            return null;
        }

        User submitter = req.getSubmittedBy();
        String name = "Unknown";
        if (submitter != null) {
            String full = (safe(submitter.getFirstName()) + " " + safe(submitter.getLastName())).trim();
            if (!full.isEmpty()) name = full;
        }

        Notification n = new Notification(
                "New audit request from " + name,
                TYPE_AUDIT_SUBMITTED,
                null,
                req.getId());
        n.setAuditType(req.getAuditType() != null ? req.getAuditType().toString() : null);
        // Anchor the timestamp to the submission so backfilled rows show the
        // correct relative time rather than "just now".
        Date submittedAt = req.getSubmittedAt();
        if (submittedAt != null) {
            n.setCreatedAt(LocalDateTime.ofInstant(submittedAt.toInstant(), ZoneId.systemDefault()));
        }
        return notificationRepository.save(n);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
