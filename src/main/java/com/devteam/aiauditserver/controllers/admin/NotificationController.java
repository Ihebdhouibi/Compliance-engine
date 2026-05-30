package com.devteam.aiauditserver.controllers.admin;

import com.devteam.aiauditserver.enums.Project.AuditStatus;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequest;
import com.devteam.aiauditserver.repositories.project.AuditRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@RestController
@CrossOrigin
@RequestMapping("/api/v1/admin/notifications")
public class NotificationController {

    @Autowired
    private AuditRequestRepository requestRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getNotifications() {
        List<AuditRequest> submitted = requestRepository
            .findByStatusOrderBySubmittedAtDesc(AuditStatus.SUBMITTED);
        List<Map<String, Object>> notifications = submitted.stream().map(r -> {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", r.getId());
            String name = r.getSubmittedBy() != null
                ? r.getSubmittedBy().getFirstName() + " " + r.getSubmittedBy().getLastName()
                : "Unknown";
            n.put("message", "New audit request from " + name.trim());
            n.put("auditType", r.getAuditType() != null ? r.getAuditType().toString() : "");
            n.put("submittedAt", r.getSubmittedAt());
            n.put("status", r.getStatus());
            return n;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        long count = requestRepository.countByStatus(AuditStatus.SUBMITTED);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
