package com.devteam.aiauditserver.controllers.admin;

import com.devteam.aiauditserver.models.project.AuditRequest.AuditJournalEntry;
import com.devteam.aiauditserver.services.project.audit.AuditJournalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Exposes the field-level change journal for an audit (changes made after it
 * was completed). Newest entries first.
 */
@RestController
@CrossOrigin
@RequestMapping("/api/v1/audits")
public class AuditJournalController {

    private final AuditJournalService journalService;

    public AuditJournalController(AuditJournalService journalService) {
        this.journalService = journalService;
    }

    @GetMapping("/{auditId}/journal")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<List<AuditJournalEntry>> getJournal(@PathVariable Long auditId) {
        return ResponseEntity.ok(journalService.getJournal(auditId));
    }
}
