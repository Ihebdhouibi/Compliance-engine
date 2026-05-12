package com.devteam.aiauditserver.services.project.ocr;

import com.devteam.aiauditserver.models.File.MediaModel;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequest;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequestAnswer;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequestAnswerFile;
import com.devteam.aiauditserver.models.project.AuditRequest.EvidenceOcrResult;
import com.devteam.aiauditserver.repositories.File.MediaRepository;
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
    private final MediaRepository mediaRepo;
    private final RestTemplate http = new RestTemplate();

    public OcrOrchestratorService(EvidenceOcrResultRepository ocrRepo,
                                  MediaRepository mediaRepo) {
        this.ocrRepo = ocrRepo;
        this.mediaRepo = mediaRepo;
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

    /**
     * Public hook: enqueue OCR for a single freshly-uploaded media file.
     * Called from `AuditRequestService.uploadAnswerFile` once the file is
     * persisted, which is the only point where evidence actually exists.
     */
    @Async
    public void enqueueMedia(Long auditId, MediaModel media) {
        enqueueOne(auditId, media);
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
        // MediaModel.url is stored as e.g. "/WebContent/audits/answers/1/file.pdf"
        // (the FilesStorageService prepends "/WebContent/"). The OCR worker
        // needs the absolute filesystem path under `upload-root` (default
        // "WebContent"), so we strip a leading "/WebContent/" prefix to
        // avoid producing "WebContent/WebContent/..." paths.
        String cleaned = relativeUrl == null ? "" : relativeUrl.replace('\\', '/');
        if (cleaned.startsWith("/")) cleaned = cleaned.substring(1);
        // Strip an optional leading "<uploadRoot>/" or "WebContent/" segment.
        String rootPrefix = (uploadRoot == null ? "WebContent" : uploadRoot)
                .replace('\\', '/');
        if (rootPrefix.endsWith("/")) rootPrefix = rootPrefix.substring(0, rootPrefix.length() - 1);
        if (cleaned.equalsIgnoreCase(rootPrefix)
                || cleaned.toLowerCase().startsWith(rootPrefix.toLowerCase() + "/")) {
            cleaned = cleaned.substring(rootPrefix.length());
            if (cleaned.startsWith("/")) cleaned = cleaned.substring(1);
        } else if (cleaned.toLowerCase().startsWith("webcontent/")) {
            cleaned = cleaned.substring("webcontent/".length());
        }
        Path p = Paths.get(uploadRoot, cleaned).toAbsolutePath().normalize();
        return p.toString();
    }

    /**
     * Re-run OCR for a single (audit, media) pair. Resets the row to
     * PENDING and POSTs a fresh job to FastAPI. Used by the admin/auditor
     * "Retry" button on a failed evidence file.
     */
    public EvidenceOcrResult retry(Long auditId, Long mediaId) {
        EvidenceOcrResult row = ocrRepo
                .findByAuditRequestIdAndMediaId(auditId, mediaId)
                .orElseThrow(() -> new RuntimeException(
                        "No OCR record for audit=" + auditId + " media=" + mediaId));

        row.setStatus(EvidenceOcrResult.OcrStatus.PENDING);
        row.setError(null);
        row.setRawText(null);
        row.setPageCount(null);
        row.setElapsedMs(null);
        row.setJobId(null);
        ocrRepo.save(row);

        try {
            MediaModel media = mediaRepo.findById(mediaId).orElseThrow(
                    () -> new RuntimeException("Media not found: " + mediaId));
            String absPath = resolveAbsolutePath(media.getUrl());

            Map<String, Object> body = new HashMap<>();
            body.put("audit_id", auditId);
            body.put("media_id", mediaId);
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
            log.info("[OCR] retried audit={} media={} jobId={}",
                    auditId, mediaId, row.getJobId());
        } catch (Exception e) {
            log.warn("[OCR] retry failed audit={} media={}: {}",
                    auditId, mediaId, e.getMessage());
            row.setStatus(EvidenceOcrResult.OcrStatus.FAILED);
            row.setError("Retry failed: " + e.getMessage());
            ocrRepo.save(row);
        }
        return row;
    }
}
