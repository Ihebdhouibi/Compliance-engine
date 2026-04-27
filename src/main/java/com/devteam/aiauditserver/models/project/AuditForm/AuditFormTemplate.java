package com.devteam.aiauditserver.models.project.AuditForm;


import com.devteam.aiauditserver.enums.Project.AuditType;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditProcessStep;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "audit_form_templates")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AuditFormTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_type", nullable = false, unique = true)
    private AuditType auditType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "created_at")
    private Date createdAt = new Date();

    @OneToMany(mappedBy = "template",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    @OrderBy("stepOrder ASC")
    @JsonManagedReference("template-steps")
    private List<AuditFormStep> steps = new ArrayList<>();

    @OneToMany(mappedBy = "template",
            cascade = CascadeType.ALL,
            orphanRemoval = false,
            fetch = FetchType.LAZY)
    @OrderBy("stepOrder ASC")
    @JsonManagedReference("template-process-steps")
    private Set<AuditProcessStep> processSteps = new HashSet<>();

    // ── Getters & Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AuditType getAuditType() { return auditType; }
    public void setAuditType(AuditType auditType) { this.auditType = auditType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public List<AuditFormStep> getSteps() { return steps; }
    public void setSteps(List<AuditFormStep> steps) { this.steps = steps; }

    public Set<AuditProcessStep> getProcessSteps() {
        return processSteps;
    }

    public void setProcessSteps(Set<AuditProcessStep> processSteps) {
        this.processSteps = processSteps;
    }
}
