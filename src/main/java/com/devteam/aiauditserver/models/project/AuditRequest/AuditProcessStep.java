package com.devteam.aiauditserver.models.project.AuditRequest;

import com.devteam.aiauditserver.models.project.AuditForm.AuditFormTemplate;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.util.Date;


@Entity
@Table(name = "audit_process_steps")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AuditProcessStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder = 1;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    @JsonBackReference("template-process-steps")
    private AuditFormTemplate template;

    @Column(name = "created_at")
    private Date createdAt = new Date();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }

    public AuditFormTemplate getTemplate() { return template; }
    public void setTemplate(AuditFormTemplate template) { this.template = template; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}