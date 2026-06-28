package com.devteam.aiauditserver.models.project.AuditRequest;

import com.devteam.aiauditserver.models.User.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.util.Date;

/**
 * One field-level change made to an audit after it was COMPLETED.
 *
 * Captures who changed what, when, and the old -> new values so a completed
 * audit's edit history can be replayed as a journal. Rows are only written
 * when an already-COMPLETED audit's step result is modified.
 */
@Entity
@Table(name = "audit_journal_entries")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AuditJournalEntry {

    public enum ChangeType { VERDICT, FINDING, RECOMMENDATION, NOTE }
    public enum Action { ADDED, MODIFIED, REMOVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_request_id", nullable = false)
    @JsonIgnoreProperties({"answers", "submittedBy", "assignedTo",
            "hibernateLazyInitializer", "handler"})
    private AuditRequest auditRequest;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "changed_by_user_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User changedBy;

    @Column(name = "changed_at")
    private Date changedAt = new Date();

    // Snapshot of the step the change belongs to
    @Column(name = "step_name")
    private String stepName;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private ChangeType changeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private Action action;

    // Reference to the specific item changed (verdict fieldId, finding/recommendation id)
    @Column(name = "field_ref")
    private String fieldRef;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    public AuditJournalEntry() {}

    public AuditJournalEntry(AuditRequest auditRequest, User changedBy, String stepName,
                             ChangeType changeType, Action action, String fieldRef,
                             String oldValue, String newValue) {
        this.auditRequest = auditRequest;
        this.changedBy = changedBy;
        this.stepName = stepName;
        this.changeType = changeType;
        this.action = action;
        this.fieldRef = fieldRef;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changedAt = new Date();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AuditRequest getAuditRequest() { return auditRequest; }
    public void setAuditRequest(AuditRequest auditRequest) { this.auditRequest = auditRequest; }

    public User getChangedBy() { return changedBy; }
    public void setChangedBy(User changedBy) { this.changedBy = changedBy; }

    public Date getChangedAt() { return changedAt; }
    public void setChangedAt(Date changedAt) { this.changedAt = changedAt; }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public ChangeType getChangeType() { return changeType; }
    public void setChangeType(ChangeType changeType) { this.changeType = changeType; }

    public Action getAction() { return action; }
    public void setAction(Action action) { this.action = action; }

    public String getFieldRef() { return fieldRef; }
    public void setFieldRef(String fieldRef) { this.fieldRef = fieldRef; }

    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }

    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
}
