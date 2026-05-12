package com.devteam.aiauditserver.controllers.admin;

import com.devteam.aiauditserver.models.project.AuditRequest.EvidenceOcrResult;
import com.devteam.aiauditserver.repositories.project.EvidenceOcrResultRepository;
import com.devteam.aiauditserver.services.project.ocr.OcrOrchestratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Read-only view of OCR results for an audit request. Used by both the
 * admin and auditor workspaces to display a badge / preview per evidence
 * file. JWT auth is enforced at the HTTP layer (PUBLIC_ENDPOINTS list).
 */
@RestController
@RequestMapping("/api/v1/audits")
public class OcrViewController {

    private final EvidenceOcrResultRepository ocrRepo;
    private final OcrOrchestratorService orchestrator;

    public OcrViewController(EvidenceOcrResultRepository ocrRepo,
                             OcrOrchestratorService orchestrator) {
        this.ocrRepo = ocrRepo;
        this.orchestrator = orchestrator;
    }

    /** All OCR rows for an audit, ordered by creation time. */
    @GetMapping("/{auditId}/ocr")
    public ResponseEntity<List<EvidenceOcrResult>> listForAudit(@PathVariable Long auditId) {
        return ResponseEntity.ok(
                ocrRepo.findByAuditRequestIdOrderByCreatedAtAsc(auditId));
    }

    /** Single OCR row for one media file. */
    @GetMapping("/{auditId}/ocr/media/{mediaId}")
    public ResponseEntity<EvidenceOcrResult> getOne(
            @PathVariable Long auditId,
            @PathVariable Long mediaId) {
        Optional<EvidenceOcrResult> row =
                ocrRepo.findByAuditRequestIdAndMediaId(auditId, mediaId);
        return row.map(ResponseEntity::ok)
                  .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Re-queue OCR for a single evidence file. */
    @PostMapping("/{auditId}/ocr/media/{mediaId}/retry")
    public ResponseEntity<EvidenceOcrResult> retry(
            @PathVariable Long auditId,
            @PathVariable Long mediaId) {
        return ResponseEntity.ok(orchestrator.retry(auditId, mediaId));
    }
}
