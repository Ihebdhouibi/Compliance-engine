package com.devteam.aiauditserver.models.project.AuditRequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.util.Date;

/**
 * Tracks the OCR processing state for a single evidence file uploaded
 * against an audit request. The actual extracted text lives in `rawText`
 * (clob) so the auditor UI can surface a preview without a second hop
 * to the Python service.
 *
 * One row per (auditRequestId, mediaId) — enforced by a unique constraint.
 */
@Entity
@Table(
    name = "evidence_ocr_results",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_evidence_ocr_audit_media",
        columnNames = {"audit_request_id", "media_id"}
    )
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class EvidenceOcrResult {

    public enum OcrStatus { PENDING, RUNNING, DONE, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "audit_request_id", nullable = false)
    private Long auditRequestId;

    @Column(name = "media_id", nullable = false)
    private Long mediaId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "job_id")
    private String jobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OcrStatus status = OcrStatus.PENDING;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "engine")
    private String engine;

    @Column(name = "elapsed_ms")
    private Integer elapsedMs;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    @Column(name = "created_at")
    private Date createdAt = new Date();

    @Column(name = "updated_at")
    private Date updatedAt = new Date();

    @PreUpdate
    public void onUpdate() { this.updatedAt = new Date(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAuditRequestId() { return auditRequestId; }
    public void setAuditRequestId(Long auditRequestId) { this.auditRequestId = auditRequestId; }

    public Long getMediaId() { return mediaId; }
    public void setMediaId(Long mediaId) { this.mediaId = mediaId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public OcrStatus getStatus() { return status; }
    public void setStatus(OcrStatus status) { this.status = status; }

    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }

    public String getEngine() { return engine; }
    public void setEngine(String engine) { this.engine = engine; }

    public Integer getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(Integer elapsedMs) { this.elapsedMs = elapsedMs; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
