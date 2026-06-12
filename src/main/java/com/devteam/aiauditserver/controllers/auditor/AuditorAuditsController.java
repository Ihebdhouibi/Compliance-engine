package com.devteam.aiauditserver.controllers.auditor;


import com.devteam.aiauditserver.Tools.util.BaseController;
import com.devteam.aiauditserver.enums.Project.AuditType;
import com.devteam.aiauditserver.models.User.User;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditProcessStep;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequest;
import com.devteam.aiauditserver.repositories.project.AuditFormTemplateRepository;
import com.devteam.aiauditserver.repositories.project.AuditProcessStepRepository;
import com.devteam.aiauditserver.requests.project.AssignAuditRequest;
import com.devteam.aiauditserver.responses.Response.DynamicResponse;
import com.devteam.aiauditserver.services.auth.UserService;
import com.devteam.aiauditserver.services.project.audit.AuditRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/v1/auditor/audits")
public class AuditorAuditsController extends BaseController {

    @Autowired
    private AuditRequestService requestService;
    @Autowired
    private UserService userService;

    @Autowired
    private AuditFormTemplateRepository templateRepository;

    @Autowired
    private AuditProcessStepRepository processStepRepository;

    // ── Get all audits assigned to me
    @GetMapping("/my-audits")
    @PreAuthorize("hasRole('AUDITOR')")
    public ResponseEntity<DynamicResponse> getMyAudits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User me = userService.findByUserName(getCurrentUser().getUsername());
        return ResponseEntity.ok(
                requestService.getMyAssignedAudits(me.getId(), page, size));
    }

    // ── Get single audit detail
    @GetMapping("/my-audits/{id}")
    @PreAuthorize("hasRole('AUDITOR')")
    public ResponseEntity<AuditRequest> getAuditDetail(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.getById(id));
    }

    // ── Start working on audit
    @PatchMapping("/my-audits/{id}/start")
    @PreAuthorize("hasRole('AUDITOR')")
    public ResponseEntity<AuditRequest> startAudit(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.startAudit(id));
    }

    // ── Mark audit as completed
    @PatchMapping("/my-audits/{id}/complete")
    @PreAuthorize("hasRole('AUDITOR')")
    public ResponseEntity<AuditRequest> completeAudit(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.completeAudit(id));
    }

    // ── Reassign audit (auditor can reassign to another auditor)
    @PatchMapping("/my-audits/{id}/reassign")
    @PreAuthorize("hasRole('AUDITOR')")
    public ResponseEntity<AuditRequest> reassignAudit(
            @PathVariable Long id,
            @RequestBody AssignAuditRequest req) {
        return ResponseEntity.ok(requestService.assignRequest(id, req));
    }

    @GetMapping("/process-steps/{auditType}")
    @PreAuthorize("hasRole('AUDITOR')")
    public ResponseEntity<List<AuditProcessStep>> getProcessStepsForAuditType(
            @PathVariable AuditType auditType) {
        return templateRepository.findByAuditType(auditType)
                .map(template -> ResponseEntity.ok(
                        processStepRepository.findByTemplateIdOrderByStepOrderAsc(template.getId())
                ))
                .orElse(ResponseEntity.ok(List.of()));
    }


}
