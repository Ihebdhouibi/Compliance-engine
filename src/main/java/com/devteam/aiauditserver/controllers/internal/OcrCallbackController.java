package com.devteam.aiauditserver.controllers.internal;

import com.devteam.aiauditserver.models.project.AuditRequest.EvidenceOcrResult;
import com.devteam.aiauditserver.repositories.project.EvidenceOcrResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * Internal callback endpoint hit by the FastAPI OCR service when a job
 * finishes. Uses a shared-secret header for authn — NOT exposed via the
 * public auth chain because the Python service is not a normal user.
 *
 * Configure with `ocr.callback-secret` (matching the Python OCR_CALLBACK_SECRET
 * env var). When unset, the endpoint is effectively closed.
 */
@RestController
@RequestMapping("/api/v1/internal/ocr")
public class OcrCallbackController {

    private static final Logger log = LoggerFactory.getLogger(OcrCallbackController.class);

    @Value("${ocr.callback-secret:}")
    private String expectedSecret;

    private final EvidenceOcrResultRepository ocrRepo;

    public OcrCallbackController(EvidenceOcrResultRepository ocrRepo) {
        this.ocrRepo = ocrRepo;
    }

    @PostMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestHeader(value = "X-OCR-Secret", required = false) String secret,
            @RequestBody Map<String, Object> payload) {

        if (expectedSecret == null || expectedSecret.isBlank()
                || !expectedSecret.equals(secret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long auditId = asLong(payload.get("auditId"));
        Long mediaId = asLong(payload.get("mediaId"));
        String jobId = asString(payload.get("jobId"));

        Optional<EvidenceOcrResult> rowOpt = Optional.empty();
        if (auditId != null && mediaId != null) {
            rowOpt = ocrRepo.findByAuditRequestIdAndMediaId(auditId, mediaId);
        }
        if (rowOpt.isEmpty() && jobId != null) {
            rowOpt = ocrRepo.findByJobId(jobId);
        }
        if (rowOpt.isEmpty()) {
            log.warn("[OCR-CALLBACK] no matching row audit={} media={} job={}",
                    auditId, mediaId, jobId);
            return ResponseEntity.notFound().build();
        }

        EvidenceOcrResult row = rowOpt.get();
        String status = asString(payload.get("status"));
        if (status != null) {
            try { row.setStatus(EvidenceOcrResult.OcrStatus.valueOf(status)); }
            catch (IllegalArgumentException ignored) { /* keep prior status */ }
        }
        row.setError(asString(payload.get("error")));
        row.setPageCount(asInt(payload.get("pageCount")));
        row.setEngine(asString(payload.get("engine")));
        row.setElapsedMs(asInt(payload.get("elapsedMs")));
        row.setRawText(asString(payload.get("text")));
        if (row.getJobId() == null && jobId != null) row.setJobId(jobId);

        ocrRepo.save(row);
        log.info("[OCR-CALLBACK] audit={} media={} status={} pages={}",
                row.getAuditRequestId(), row.getMediaId(),
                row.getStatus(), row.getPageCount());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/audits/{auditId}")
    public ResponseEntity<?> listForAudit(
            @RequestHeader(value = "X-OCR-Secret", required = false) String secret,
            @PathVariable Long auditId) {
        if (expectedSecret == null || expectedSecret.isBlank()
                || !expectedSecret.equals(secret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(
                ocrRepo.findByAuditRequestIdOrderByCreatedAtAsc(auditId));
    }

    // ── helpers ───────────────────────────────────────────────────────────
    private static Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }
    private static Integer asInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return null; }
    }
    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }
}
