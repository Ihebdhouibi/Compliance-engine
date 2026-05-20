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

    @Column(name = "label", nullable = false, columnDefinition = "TEXT")
    private String label;

    @Column(name = "placeholder", columnDefinition = "TEXT")
    private String placeholder;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false)
    private FieldType fieldType;

    @Column(name = "required")
    private Boolean required = false;

    /** When fieldType=FILE, allows multiple files per answer. Ignored for other field types. */
    @Column(name = "multiple_files")
    private Boolean multipleFiles = false;

    // ── Two-level metadata (nullable for legacy fields) ──────────────────

    /** Stable key from the seed JSON, used to look up answers across template versions. */
    @Column(name = "field_key", length = 120)
    private String fieldKey;

    /** Key into the AuditFormOptionList table for dropdown / radio / multi-checkbox fields. */
    @Column(name = "option_source_key", length = 120)
    private String optionSourceKey;

    /** JSON expression describing when the field is visible (see RoutingEngine). */
    @Column(name = "visibility_rule", columnDefinition = "TEXT")
    private String visibilityRule;

    /** JSON array of routing tags used by the L1 RoutingEngine. */
    @Column(name = "routing_tags", columnDefinition = "TEXT")
    private String routingTags;

    /** L2 only — RICS clause reference. */
    @Column(name = "rics_clause", length = 80)
    private String ricsClause;

    /** L2 only — Module the question belongs to (Governance, DataPrivacy, …). */
    @Column(name = "module", length = 80)
    private String module;

    /** L2 only — Category grouping (e.g. "AI Governance & Strategy"). */
    @Column(name = "category", length = 160)
    private String category;

    /** L2 only — When this question applies, e.g. "All", "Providers only". */
    @Column(name = "applicability_trigger", length = 200)
    private String applicabilityTrigger;

    /** L2 only — Required evidence depth (LOW/MEDIUM/HIGH). */
    @Column(name = "evidence_depth", length = 40)
    private String evidenceDepth;

    /** L2 only — Question priority. */
    @Column(name = "priority", length = 40)
    private String priority;

    /** L2 only — Expected evidence / assets description. */
    @Column(name = "expected_evidence", columnDefinition = "TEXT")
    private String expectedEvidence;

    /** L2 only — Rationale / notes for the auditor. */
    @Column(name = "rationale", columnDefinition = "TEXT")
    private String rationale;

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

    public Boolean getMultipleFiles() { return multipleFiles; }
    public void setMultipleFiles(Boolean multipleFiles) { this.multipleFiles = multipleFiles; }

    public AuditFormStep getStep() { return step; }
    public void setStep(AuditFormStep step) { this.step = step; }

    public List<AuditFormFieldOption> getOptions() { return options; }
    public void setOptions(List<AuditFormFieldOption> options) { this.options = options; }

    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }

    public String getOptionSourceKey() { return optionSourceKey; }
    public void setOptionSourceKey(String optionSourceKey) { this.optionSourceKey = optionSourceKey; }

    public String getVisibilityRule() { return visibilityRule; }
    public void setVisibilityRule(String visibilityRule) { this.visibilityRule = visibilityRule; }

    public String getRoutingTags() { return routingTags; }
    public void setRoutingTags(String routingTags) { this.routingTags = routingTags; }

    public String getRicsClause() { return ricsClause; }
    public void setRicsClause(String ricsClause) { this.ricsClause = ricsClause; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getApplicabilityTrigger() { return applicabilityTrigger; }
    public void setApplicabilityTrigger(String applicabilityTrigger) { this.applicabilityTrigger = applicabilityTrigger; }

    public String getEvidenceDepth() { return evidenceDepth; }
    public void setEvidenceDepth(String evidenceDepth) { this.evidenceDepth = evidenceDepth; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getExpectedEvidence() { return expectedEvidence; }
    public void setExpectedEvidence(String expectedEvidence) { this.expectedEvidence = expectedEvidence; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
}