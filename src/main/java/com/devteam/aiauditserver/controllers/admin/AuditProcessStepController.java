package com.devteam.aiauditserver.controllers.admin;


import com.devteam.aiauditserver.Tools.util.BaseController;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditProcessStep;
import com.devteam.aiauditserver.requests.project.CreateAuditProcessStepRequest;
import com.devteam.aiauditserver.services.project.audit.AuditProcessStepService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.devteam.aiauditserver.Tools.util.BaseController;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditProcessStep;
import com.devteam.aiauditserver.requests.project.CreateAuditProcessStepRequest;
import com.devteam.aiauditserver.services.project.audit.AuditProcessStepService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/v1/audit-templates/{templateId}/process-steps")
public class AuditProcessStepController extends BaseController {

    @Autowired
    private AuditProcessStepService stepService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<List<AuditProcessStep>> getSteps(
            @PathVariable Long templateId) {
        return ResponseEntity.ok(
                stepService.getStepsForTemplate(templateId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditProcessStep> addStep(
            @PathVariable Long templateId,
            @RequestBody CreateAuditProcessStepRequest req) {
        return new ResponseEntity<>(
                stepService.addStep(templateId, req), HttpStatus.CREATED);
    }

    @PutMapping("/{stepId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditProcessStep> updateStep(
            @PathVariable Long templateId,
            @PathVariable Long stepId,
            @RequestBody CreateAuditProcessStepRequest req) {
        return ResponseEntity.ok(stepService.updateStep(stepId, req));
    }

    @DeleteMapping("/{stepId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStep(
            @PathVariable Long templateId,
            @PathVariable Long stepId) {
        stepService.deleteStep(stepId);
        return ResponseEntity.noContent().build();
    }
}