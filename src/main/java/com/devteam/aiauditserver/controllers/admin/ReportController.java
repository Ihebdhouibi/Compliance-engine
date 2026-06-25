package com.devteam.aiauditserver.controllers.admin;

import com.devteam.aiauditserver.services.ReportService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;

/**
 * Generates a downloadable PDF report for a completed audit. JWT auth is
 * enforced at the HTTP layer (PUBLIC_ENDPOINTS list), like the other
 * /api/v1/audits endpoints.
 */
@RestController
@RequestMapping("/api/v1/audits")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** Build and stream the audit PDF report. */
    @GetMapping("/{auditId}/report")
    public ResponseEntity<InputStreamResource> exportReport(@PathVariable Long auditId) {
        ByteArrayInputStream pdf = reportService.generateAuditReport(auditId);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=audit_" + auditId + "_report.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdf));
    }
}
