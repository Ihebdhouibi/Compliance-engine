package com.devteam.aiauditserver.Tools.util;

import com.devteam.aiauditserver.enums.Project.AuditStatus;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequest;
import com.devteam.aiauditserver.repositories.NotificationRepository;
import com.devteam.aiauditserver.repositories.project.AuditRequestRepository;
import com.devteam.aiauditserver.services.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * One-time migration: seed the persisted notifications feed from audit requests
 * that were already SUBMITTED before notifications became persistent. Runs only
 * when the notifications table is empty, so it never resurrects rows an admin
 * later deletes — going forward, notifications are created on audit submission.
 */
@Component
@Order(20)
public class NotificationBackfillRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(NotificationBackfillRunner.class);

    private final NotificationRepository notificationRepository;
    private final AuditRequestRepository requestRepository;
    private final NotificationService notificationService;

    public NotificationBackfillRunner(NotificationRepository notificationRepository,
                                      AuditRequestRepository requestRepository,
                                      NotificationService notificationService) {
        this.notificationRepository = notificationRepository;
        this.requestRepository = requestRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            if (notificationRepository.count() > 0) return; // migrate once only
            List<AuditRequest> submitted =
                    requestRepository.findByStatusOrderBySubmittedAtDesc(AuditStatus.SUBMITTED);
            int created = 0;
            for (AuditRequest r : submitted) {
                if (notificationService.notifyAuditSubmitted(r) != null) created++;
            }
            logger.info("Notification backfill: created {} notification(s) from {} submitted audit request(s)",
                    created, submitted.size());
        } catch (Exception e) {
            logger.error("Notification backfill failed", e);
        }
    }
}
