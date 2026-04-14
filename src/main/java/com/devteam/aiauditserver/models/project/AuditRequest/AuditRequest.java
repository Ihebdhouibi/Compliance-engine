package com.devteam.aiauditserver.models.project.AuditRequest;

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