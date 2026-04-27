package com.devteam.aiauditserver.controllers.auditor;


import com.devteam.aiauditserver.Tools.util.BaseController;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditProcessStep;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequest;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditStepResult;
import com.devteam.aiauditserver.requests.project.SaveStepResultRequest;
import com.devteam.aiauditserver.services.project.audit.AuditProcessStepService;
import com.devteam.aiauditserver.services.project.audit.AuditRequestService;
import com.devteam.aiauditserver.services.project.audit.AuditStepResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/v1/audit-requests/{requestId}/step-results")
public class AuditStepResultController extends BaseController {

    @Autowired
    private AuditStepResultService resultService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'USER')")
    public ResponseEntity<List<AuditStepResult>> getResults(
            @PathVariable Long requestId) {
        return ResponseEntity.ok(
                resultService.getResultsForRequest(requestId));
    }

    // POST = upsert (create or update for same request+step)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<AuditStepResult> saveOrUpdate(
            @PathVariable Long requestId,
            @RequestBody SaveStepResultRequest req) {
        String username = getCurrentUser().getUsername();
        return ResponseEntity.ok(
                resultService.saveOrUpdate(requestId, req, username));
    }

    // PUT = explicit update by result ID
    @PutMapping("/{resultId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<AuditStepResult> update(
            @PathVariable Long requestId,
            @PathVariable Long resultId,
            @RequestBody SaveStepResultRequest req) {
        return ResponseEntity.ok(
                resultService.updateResult(resultId, req));
    }

    @DeleteMapping("/{resultId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<Void> delete(
            @PathVariable Long requestId,
            @PathVariable Long resultId) {
        resultService.deleteResult(resultId);
        return ResponseEntity.noContent().build();
    }
}