package com.devteam.aiauditserver.models.project.AuditRequest;

import com.devteam.aiauditserver.models.File.MediaModel;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;

@Entity
@Table(name = "audit_request_answers")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AuditRequestAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "field_id", nullable = false)
    private Long fieldId;

    @Column(name = "field_label", nullable = false)
    private String fieldLabel;

    // Text answer — for all non-file fields
    // For MULTI_CHECKBOX stored as comma-separated values
    @Column(name = "answer_value", columnDefinition = "TEXT")
    private String answerValue;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "file_media_id")
    private MediaModel fileMedia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_request_id", nullable = false)
    @JsonBackReference("request-answers")
    private AuditRequest auditRequest;

    // ── Getters & Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFieldId() { return fieldId; }
    public void setFieldId(Long fieldId) { this.fieldId = fieldId; }

    public String getFieldLabel() { return fieldLabel; }
    public void setFieldLabel(String fieldLabel) { this.fieldLabel = fieldLabel; }

    public String getAnswerValue() { return answerValue; }
    public void setAnswerValue(String answerValue) { this.answerValue = answerValue; }

    public MediaModel getFileMedia() { return fileMedia; }
    public void setFileMedia(MediaModel fileMedia) { this.fileMedia = fileMedia; }

    public AuditRequest getAuditRequest() { return auditRequest; }
    public void setAuditRequest(AuditRequest auditRequest) { this.auditRequest = auditRequest; }
}