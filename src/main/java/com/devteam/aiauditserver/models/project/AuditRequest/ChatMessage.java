package com.devteam.aiauditserver.models.project.AuditRequest;


import com.devteam.aiauditserver.models.User.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.util.Date;

/**
 * One turn of the AI Audit Assistant conversation, persisted and linked to the
 * audit under review so the discussion is restored whenever the audit is
 * reopened. The LLM/RAG call itself still runs in the Python FastAPI service;
 * only the transcript is stored here.
 */
@Entity
@Table(name = "audit_chat_messages")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChatMessage {

    public enum MessageRole { USER, ASSISTANT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Direct FK to AuditRequest — queried by requestId via repository
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_request_id", nullable = false)
    @JsonIgnoreProperties({"answers", "submittedBy", "assignedTo",
            "hibernateLazyInitializer", "handler"})
    private AuditRequest auditRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private MessageRole role;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    // Optional JSON-encoded RAG sources returned alongside an assistant reply
    @Column(name = "sources", columnDefinition = "TEXT")
    private String sources;

    // Session owner who triggered the turn (nullable if user cannot be resolved)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_user_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User author;

    @Column(name = "created_at")
    private Date createdAt = new Date();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AuditRequest getAuditRequest() { return auditRequest; }
    public void setAuditRequest(AuditRequest auditRequest) {
        this.auditRequest = auditRequest;
    }

    public MessageRole getRole() { return role; }
    public void setRole(MessageRole role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSources() { return sources; }
    public void setSources(String sources) { this.sources = sources; }

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
