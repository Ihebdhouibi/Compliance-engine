package com.devteam.aiauditserver.models.project.AuditRequest;

import com.devteam.aiauditserver.enums.Project.AuditPhase;
import com.devteam.aiauditserver.enums.Project.AuditStatus;
import com.devteam.aiauditserver.enums.Project.AuditType;
import com.devteam.aiauditserver.models.User.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "audit_requests")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AuditRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_type", nullable = false)
    private AuditType auditType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AuditStatus status = AuditStatus.SUBMITTED;

    /**
     * Two-level lifecycle phase. Defaults to LEGACY for any audit that pre-dates
     * the two-level feature. New audits started via the Level 1 flow will set
     * this to DRAFT_L1 and advance from there.
     */
    // NOTE: column is nullable at the schema level so Hibernate hbm2ddl=update
    // can add it to pre-existing audit_requests rows without violating a
    // NOT NULL constraint. SchemaUpgrader back-fills legacy rows with 'LEGACY'
    // on startup, and the Java default ensures new rows always have a value.
    @Enumerated(EnumType.STRING)
    @Column(name = "phase")
    private AuditPhase phase = AuditPhase.LEGACY;

    /** Resolved L1 template id (snapshot — survives template re-seeding). */
    @Column(name = "level1_template_id")
    private Long level1TemplateId;

    /** Resolved L2 template id (snapshot — survives template re-seeding). */
    @Column(name = "level2_template_id")
    private Long level2TemplateId;

    // The company user who submitted the request
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "submitted_by_user_id", nullable = false)
    private User submittedBy;

    // The auditor (or admin) assigned to handle this request
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo;

    @Column(name = "due_date")
    private Date dueDate;

    @Column(name = "submitted_at")
    private Date submittedAt = new Date();

    @Column(name = "assigned_at")
    private Date assignedAt;

    @Column(name = "completed_at")
    private Date completedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    // Answers provided by the user when filling the form
    @OneToMany(mappedBy = "auditRequest",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    @JsonManagedReference("request-answers")
    private List<AuditRequestAnswer> answers = new ArrayList<>();

    // ── Getters & Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AuditType getAuditType() { return auditType; }
    public void setAuditType(AuditType auditType) { this.auditType = auditType; }

    public AuditStatus getStatus() { return status; }
    public void setStatus(AuditStatus status) { this.status = status; }

    public AuditPhase getPhase() { return phase; }
    public void setPhase(AuditPhase phase) { this.phase = phase; }

    public Long getLevel1TemplateId() { return level1TemplateId; }
    public void setLevel1TemplateId(Long level1TemplateId) { this.level1TemplateId = level1TemplateId; }

    public Long getLevel2TemplateId() { return level2TemplateId; }
    public void setLevel2TemplateId(Long level2TemplateId) { this.level2TemplateId = level2TemplateId; }

    public User getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(User submittedBy) { this.submittedBy = submittedBy; }

    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }

    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }

    public Date getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Date submittedAt) { this.submittedAt = submittedAt; }

    public Date getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Date assignedAt) { this.assignedAt = assignedAt; }

    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public List<AuditRequestAnswer> getAnswers() { return answers; }
    public void setAnswers(List<AuditRequestAnswer> answers) { this.answers = answers; }
}
