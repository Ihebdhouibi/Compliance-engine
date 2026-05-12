package com.devteam.aiauditserver.services.project.ocr;

import com.devteam.aiauditserver.models.File.MediaModel;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequest;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequestAnswer;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequestAnswerFile;
import com.devteam.aiauditserver.models.project.AuditRequest.EvidenceOcrResult;
import com.devteam.aiauditserver.repositories.project.EvidenceOcrResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring-side orchestrator: when an audit is submitted, walks the evidence
 * attachments and asks the FastAPI OCR service to process each one.
 *
 * Step-1 implementation:
 *  - persists a PENDING EvidenceOcrResult row per (auditId, mediaId)
 *  - POSTs to ${ocr.base-url}/ocr/jobs
 *  - stores the returned jobId on the row
 *  - actual extraction completes asynchronously and is delivered via the
 *    OcrCallbackController callback.
 */
@Service
public class OcrOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(OcrOrchestratorService.class);

    @Value("${ocr.base-url:http://localhost:8000}")
    private String ocrBaseUrl;

    /**
     * Folder where uploaded evidence is written on disk. Defaults to the
     * `WebContent` folder used by the existing static-resource handler.
     * The FastAPI worker must be able to read the same path; on a single
     * host this is the same absolute filesystem path.
     */
    @Value("${ocr.upload-root:WebContent}")
    private String uploadRoot;

    private final EvidenceOcrResultRepository ocrRepo;
    private final RestTemplate http = new RestTemplate();

    public OcrOrchestratorService(EvidenceOcrResultRepository ocrRepo) {
        this.ocrRepo = ocrRepo;
    }

    /**
     * Fire-and-forget OCR submission for all evidence files on a request.
     * Called after `AuditRequestService.submitRequest` persists the audit.
     */
    @Async
    public void submitEvidenceForOcr(AuditRequest request) {
        if (request == null || request.getAnswers() == null) return;
        for (AuditRequestAnswer answer : request.getAnswers()) {
            if (answer.getFileMedia() != null) {
                enqueueOne(request.getId(), answer.getFileMedia());
            }
            if (answer.getFiles() != null) {
                for (AuditRequestAnswerFile f : answer.getFiles()) {
                    if (f.getMedia() != null) enqueueOne(request.getId(), f.getMedia());
                }
            }
        }
    }

    private void enqueueOne(Long auditId, MediaModel media) {
        if (media == null || media.getId() == null) return;

        // Idempotency: skip if a row already exists (replays, retries, etc.)
        if (ocrRepo.findByAuditRequestIdAndMediaId(auditId, media.getId()).isPresent()) {
            return;
        }

        EvidenceOcrResult row = new EvidenceOcrResult();
        row.setAuditRequestId(auditId);
        row.setMediaId(media.getId());
        row.setFileName(media.getName());
        row.setMimeType(media.getType());
        row.setStatus(EvidenceOcrResult.OcrStatus.PENDING);
        ocrRepo.save(row);

        try {
            String absPath = resolveAbsolutePath(media.getUrl());
            Map<String, Object> body = new HashMap<>();
            body.put("audit_id", auditId);
            body.put("media_id", media.getId());
            body.put("file_path", absPath);
            body.put("mime", media.getType());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = http.postForObject(
                    ocrBaseUrl + "/ocr/jobs",
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            if (resp != null && resp.get("id") != null) {
                row.setJobId(resp.get("id").toString());
                ocrRepo.save(row);
            }
            log.info("[OCR] enqueued audit={} media={} jobId={}",
                    auditId, media.getId(), row.getJobId());
        } catch (Exception e) {
            log.warn("[OCR] failed to enqueue audit={} media={}: {}",
                    auditId, media.getId(), e.getMessage());
            row.setStatus(EvidenceOcrResult.OcrStatus.FAILED);
            row.setError("Enqueue failed: " + e.getMessage());
            ocrRepo.save(row);
        }
    }

    private String resolveAbsolutePath(String relativeUrl) {
        // MediaModel.url is stored as e.g. "/audits/answers/1/file.pdf".
        // The static resource handler maps "/<uploadRoot>/**" → "<uploadRoot>/**".
        // The OCR worker needs the absolute filesystem path.
        String cleaned = relativeUrl == null ? "" : relativeUrl;
        if (cleaned.startsWith("/")) cleaned = cleaned.substring(1);
        Path p = Paths.get(uploadRoot, cleaned).toAbsolutePath().normalize();
        return p.toString();
    }
}
