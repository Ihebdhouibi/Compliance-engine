package com.devteam.aiauditserver.services.project.ocr;

import com.devteam.aiauditserver.config.CorrelationIdFilter;
import com.devteam.aiauditserver.models.File.MediaModel;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequest;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequestAnswer;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequestAnswerFile;
import com.devteam.aiauditserver.models.project.AuditRequest.EvidenceOcrResult;
import com.devteam.aiauditserver.repositories.File.MediaRepository;
import com.devteam.aiauditserver.repositories.project.EvidenceOcrResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Spring-side orchestrator: when an audit is submitted, walks the evidence
 * attachments and asks the FastAPI OCR service to process each one.
 *
 * Synchronous implementation:
 *  - Sends the actual file content to ${ocr.base-url}/ocr/upload
 *  - Receives extracted text immediately and stores it in the database.
 *  - No callbacks, no polling required.
 */
@Service
public class OcrOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger("spring.ocr-orchestrator");

    @Value("${ocr.base-url:http://localhost:8000}")
    private String ocrBaseUrl;

    /**
     * Folder where uploaded evidence is written on disk. Defaults to the
     * `WebContent` folder used by the existing static-resource handler.
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
     * persisted.
     */
    @Async
    public void enqueueMedia(Long auditId, MediaModel media) {
        enqueueOne(auditId, media);
    }

    /**
     * Synchronously sends a file to Python's /ocr/upload endpoint,
     * receives the extracted text, and immediately updates the database.
     */
    private void enqueueOne(Long auditId, MediaModel media) {
        if (media == null || media.getId() == null) return;

        // Idempotency: if already DONE, skip; if PENDING/FAILED, proceed.
        EvidenceOcrResult row = ocrRepo.findByAuditRequestIdAndMediaId(auditId, media.getId())
                .orElse(null);
        if (row == null) {
            row = new EvidenceOcrResult();
            row.setAuditRequestId(auditId);
            row.setMediaId(media.getId());
            row.setFileName(media.getName());
            row.setMimeType(media.getType());
            row.setStatus(EvidenceOcrResult.OcrStatus.PENDING);
            ocrRepo.save(row);
        } else if (row.getStatus() == EvidenceOcrResult.OcrStatus.DONE) {
            log.info("[OCR] already DONE for audit={} media={}", auditId, media.getId());
            return;
        } else {
            // Reset for retry or fresh processing
            row.setStatus(EvidenceOcrResult.OcrStatus.PENDING);
            row.setError(null);
            row.setRawText(null);
            row.setPageCount(null);
            row.setElapsedMs(null);
            ocrRepo.save(row);
        }

        try {
            // Get absolute file path from media URL
            String absPath = resolveAbsolutePath(media.getUrl());
            File file = new File(absPath);
            if (!file.exists()) {
                throw new RuntimeException("File not found: " + absPath);
            }

            // Build multipart request
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(file));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            // Forward the correlation id so the FastAPI OCR logs share this cid.
            String cid = MDC.get(CorrelationIdFilter.MDC_KEY);
            if (cid != null) {
                headers.add(CorrelationIdFilter.HEADER, cid);
            }

            log.info("[OCR] upload > audit={} media={} file={}", auditId, media.getId(), media.getName());

            // Call Python's synchronous /upload endpoint
            @SuppressWarnings("unchecked")
            Map<String, Object> response = http.postForObject(
                    ocrBaseUrl + "/ocr/upload",
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            if (response != null && response.containsKey("text")) {
                String extractedText = (String) response.get("text");
                Integer pageCount = (Integer) response.get("page_count");
                Integer elapsedMs = (Integer) response.get("elapsed_ms");

                row.setRawText(extractedText);
                row.setPageCount(pageCount != null ? pageCount : 0);
                row.setElapsedMs(elapsedMs != null ? elapsedMs : 0);
                row.setStatus(EvidenceOcrResult.OcrStatus.DONE);
                ocrRepo.save(row);

                log.info("[OCR] completed audit={} media={} pages={} elapsedMs={}",
                        auditId, media.getId(), pageCount, elapsedMs);
            } else {
                throw new RuntimeException("Invalid response from OCR service");
            }
        } catch (Exception e) {
            log.warn("[OCR] failed audit={} media={}: {}", auditId, media.getId(), e.getMessage());
            row.setStatus(EvidenceOcrResult.OcrStatus.FAILED);
            row.setError("OCR failed: " + e.getMessage());
            ocrRepo.save(row);
        }
    }

    /**
     * Re-run OCR for a single (audit, media) pair. Resets the row to
     * PENDING and processes synchronously.
     */
    public EvidenceOcrResult retry(Long auditId, Long mediaId) {
        MediaModel media = mediaRepo.findById(mediaId)
                .orElseThrow(() -> new RuntimeException("Media not found: " + mediaId));
        enqueueOne(auditId, media);
        return ocrRepo.findByAuditRequestIdAndMediaId(auditId, mediaId)
                .orElseThrow(() -> new RuntimeException("OCR record not found after retry"));
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
}