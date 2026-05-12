package com.devteam.aiauditserver.models.project.AuditRequest;

import com.devteam.aiauditserver.models.File.MediaModel;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "audit_request_answers")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AuditRequestAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "field_id", nullable = false)
    private Long fieldId;

    @Column(name = "field_label", nullable = false, columnDefinition = "TEXT")
    private String fieldLabel;

    // Text answer — for all non-file fields
    // For MULTI_CHECKBOX stored as comma-separated values
    @Column(name = "answer_value", columnDefinition = "TEXT")
    private String answerValue;

    /** Legacy single-file slot. Used when the underlying field is FILE with multipleFiles=false. */
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "file_media_id")
    private MediaModel fileMedia;

    /** Multi-file slot. Used when the underlying field is FILE with multipleFiles=true. */
    @OneToMany(mappedBy = "answer",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    @OrderBy("fileOrder ASC")
    @JsonManagedReference("answer-files")
    private List<AuditRequestAnswerFile> files = new ArrayList<>();

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

    public List<AuditRequestAnswerFile> getFiles() { return files; }
    public void setFiles(List<AuditRequestAnswerFile> files) { this.files = files; }

    public AuditRequest getAuditRequest() { return auditRequest; }
    public void setAuditRequest(AuditRequest auditRequest) { this.auditRequest = auditRequest; }
}