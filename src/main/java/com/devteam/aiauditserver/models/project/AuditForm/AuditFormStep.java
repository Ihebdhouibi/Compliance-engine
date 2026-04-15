package com.devteam.aiauditserver.models.project.AuditForm;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "audit_form_steps")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AuditFormStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    @JsonBackReference("template-steps")
    private AuditFormTemplate template;

    @OneToMany(mappedBy = "step",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    @OrderBy("fieldOrder ASC")
    @JsonManagedReference("step-fields")
    private List<AuditFormField> fields = new ArrayList<>();

    // ── Getters & Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public AuditFormTemplate getTemplate() { return template; }
    public void setTemplate(AuditFormTemplate template) { this.template = template; }

    public List<AuditFormField> getFields() { return fields; }
    public void setFields(List<AuditFormField> fields) { this.fields = fields; }
}
