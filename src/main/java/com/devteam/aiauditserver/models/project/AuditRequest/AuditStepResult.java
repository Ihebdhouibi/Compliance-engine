package com.devteam.aiauditserver.models.project.AuditRequest;


import com.devteam.aiauditserver.models.User.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "audit_step_results")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AuditStepResult {

    public enum StepResultStatus { DRAFT, SAVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Snapshot — always preserved even if processStep is deleted later
    @Column(name = "step_name", nullable = false)
    private String stepName;

    // Nullable: if admin deletes the process step, FK becomes null
    // but stepName snapshot above is always preserved
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "process_step_id", nullable = true)
    @JsonIgnoreProperties({"template", "hibernateLazyInitializer", "handler"})
    private AuditProcessStep processStep;

    // Direct FK to AuditRequest — no back reference needed here
    // We query results by requestId via repository, not via AuditRequest.stepResults
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_request_id", nullable = false)
    @JsonIgnoreProperties({"answers", "submittedBy", "assignedTo",
            "hibernateLazyInitializer", "handler"})
    private AuditRequest auditRequest;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StepResultStatus status = StepResultStatus.DRAFT;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "filled_by_user_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User filledBy;

    @Column(name = "created_at")
    private Date createdAt = new Date();

    @Column(name = "updated_at")
    private Date updatedAt = new Date();

    @PreUpdate
    public void onUpdate() { this.updatedAt = new Date(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public AuditProcessStep getProcessStep() { return processStep; }
    public void setProcessStep(AuditProcessStep processStep) {
        this.processStep = processStep;
    }

    public AuditRequest getAuditRequest() { return auditRequest; }
    public void setAuditRequest(AuditRequest auditRequest) {
        this.auditRequest = auditRequest;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public StepResultStatus getStatus() { return status; }
    public void setStatus(StepResultStatus status) { this.status = status; }

    public User getFilledBy() { return filledBy; }
    public void setFilledBy(User filledBy) { this.filledBy = filledBy; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}