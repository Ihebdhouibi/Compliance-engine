package com.devteam.aiauditserver.models;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String message;
    private String type;
    private Long recipientUserId;
    private Long auditRequestId;
    private boolean read;
    private LocalDateTime createdAt;

    public Notification() {}
    public Notification(String message, String type, Long recipientUserId, Long auditRequestId) {
        this.message = message;
        this.type = type;
        this.recipientUserId = recipientUserId;
        this.auditRequestId = auditRequestId;
        this.read = false;
        this.createdAt = LocalDateTime.now();
    }

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(Long recipientUserId) { this.recipientUserId = recipientUserId; }
    public Long getAuditRequestId() { return auditRequestId; }
    public void setAuditRequestId(Long auditRequestId) { this.auditRequestId = auditRequestId; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
