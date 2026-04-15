package com.devteam.aiauditserver.models.project.AuditForm;


import com.devteam.aiauditserver.enums.Project.FieldType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "audit_form_fields")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AuditFormField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "field_order", nullable = false)
    private Integer fieldOrder;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "placeholder")
    private String placeholder;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false)
    private FieldType fieldType;

    @Column(name = "required")
    private Boolean required = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    @JsonBackReference("step-fields")
    private AuditFormStep step;

    @OneToMany(
            mappedBy = "field",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @OrderBy("optionOrder ASC")
    @JsonManagedReference("field-options")
    private List<AuditFormFieldOption> options = new ArrayList<>();

    public AuditFormField() {}

    // ── Getters & Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getFieldOrder() { return fieldOrder; }
    public void setFieldOrder(Integer fieldOrder) { this.fieldOrder = fieldOrder; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getPlaceholder() { return placeholder; }
    public void setPlaceholder(String placeholder) { this.placeholder = placeholder; }

    public FieldType getFieldType() { return fieldType; }
    public void setFieldType(FieldType fieldType) { this.fieldType = fieldType; }

    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }

    public AuditFormStep getStep() { return step; }
    public void setStep(AuditFormStep step) { this.step = step; }

    public List<AuditFormFieldOption> getOptions() { return options; }
    public void setOptions(List<AuditFormFieldOption> options) { this.options = options; }
}