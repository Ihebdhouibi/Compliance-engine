package com.devteam.aiauditserver.models.project.AuditRequest;

import com.devteam.aiauditserver.models.User.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.util.Date;

/**
 * Auditor scoring + evidence verdict for a single Level-2 answer.
 * Decoupled from {@link AuditRequestAnswer} so scoring history is preserved
 * if the answer is later edited and so multiple auditors could co-sign a
 * single answer in the future.
 */
@Entity
@Table(name = "audit_answer_scorings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_scoring_per_answer",
                columnNames = {"answer_id"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AuditAnswerScoring {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false)
    private AuditRequestAnswer answer;

    /** 0..5 maturity score. */
    @Column(name = "auditor_score")
    private Integer auditorScore;

    @Column(name = "auditor_notes", columnDefinition = "TEXT")
    private String auditorNotes;

    /** PROVIDED | INSUFFICIENT | MISSING | NOT_APPLICABLE */
    @Column(name = "evidence_provided_status", length = 30)
    private String evidenceProvidedStatus;

    /** Free-text evidence reference (file name, URL, document id). */
    @Column(name = "evidence_reference", columnDefinition = "TEXT")
    private String evidenceReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scored_by_user_id")
    private User scoredBy;

    @Column(name = "scored_at")
    private Date scoredAt = new Date();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AuditRequestAnswer getAnswer() { return answer; }
    public void setAnswer(AuditRequestAnswer answer) { this.answer = answer; }

    public Integer getAuditorScore() { return auditorScore; }
    public void setAuditorScore(Integer auditorScore) { this.auditorScore = auditorScore; }

    public String getAuditorNotes() { return auditorNotes; }
    public void setAuditorNotes(String auditorNotes) { this.auditorNotes = auditorNotes; }

    public String getEvidenceProvidedStatus() { return evidenceProvidedStatus; }
    public void setEvidenceProvidedStatus(String evidenceProvidedStatus) { this.evidenceProvidedStatus = evidenceProvidedStatus; }

    public String getEvidenceReference() { return evidenceReference; }
    public void setEvidenceReference(String evidenceReference) { this.evidenceReference = evidenceReference; }

    public User getScoredBy() { return scoredBy; }
    public void setScoredBy(User scoredBy) { this.scoredBy = scoredBy; }

    public Date getScoredAt() { return scoredAt; }
    public void setScoredAt(Date scoredAt) { this.scoredAt = scoredAt; }
}
