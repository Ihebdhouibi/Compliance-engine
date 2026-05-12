package com.devteam.aiauditserver.models.project.AuditRequest;

import com.devteam.aiauditserver.models.File.MediaModel;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;

/**
 * One uploaded file attached to a single {@link AuditRequestAnswer}.
 * Used for FILE fields where the underlying {@code AuditFormField.multipleFiles == true}.
 *
 * For legacy single-file answers we keep {@link AuditRequestAnswer#getFileMedia()};
 * multi-file answers populate this list instead.
 */
@Entity
@Table(name = "audit_request_answer_files")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AuditRequestAnswerFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "media_id", nullable = false)
    private MediaModel media;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false)
    @JsonBackReference("answer-files")
    private AuditRequestAnswer answer;

    @Column(name = "file_order")
    private Integer fileOrder = 0;

    public AuditRequestAnswerFile() {}

    public AuditRequestAnswerFile(MediaModel media, AuditRequestAnswer answer, Integer fileOrder) {
        this.media = media;
        this.answer = answer;
        this.fileOrder = fileOrder;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MediaModel getMedia() { return media; }
    public void setMedia(MediaModel media) { this.media = media; }

    public AuditRequestAnswer getAnswer() { return answer; }
    public void setAnswer(AuditRequestAnswer answer) { this.answer = answer; }

    public Integer getFileOrder() { return fileOrder; }
    public void setFileOrder(Integer fileOrder) { this.fileOrder = fileOrder; }
}
