package com.devteam.aiauditserver.controllers.admin;

import com.devteam.aiauditserver.models.Notification;
import com.devteam.aiauditserver.requests.project.NotificationIdsRequest;
import com.devteam.aiauditserver.services.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/v1/admin/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Notification>> getNotifications() {
        return ResponseEntity.ok(notificationService.listAll());
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount()));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markOneRead(@PathVariable Long id) {
        notificationService.markRead(Collections.singletonList(id));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markRead(@RequestBody NotificationIdsRequest body) {
        notificationService.markRead(body.getIds());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markAllRead() {
        notificationService.markAllRead();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOne(@PathVariable Long id) {
        notificationService.delete(Collections.singletonList(id));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBatch(@RequestBody NotificationIdsRequest body) {
        notificationService.delete(body.getIds());
        return ResponseEntity.noContent().build();
    }
}
