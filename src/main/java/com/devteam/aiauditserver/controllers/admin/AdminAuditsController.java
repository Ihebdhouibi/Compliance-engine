package com.devteam.aiauditserver.controllers.admin;


import com.devteam.aiauditserver.Tools.util.BaseController;
import com.devteam.aiauditserver.enums.Project.AuditStatus;
import com.devteam.aiauditserver.enums.Project.AuditType;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormField;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormStep;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormTemplate;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditProcessStep;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequest;
import com.devteam.aiauditserver.repositories.project.AuditFormFieldRepository;
import com.devteam.aiauditserver.repositories.project.AuditFormStepRepository;
import com.devteam.aiauditserver.repositories.project.AuditFormTemplateRepository;
import com.devteam.aiauditserver.repositories.project.AuditProcessStepRepository;
import com.devteam.aiauditserver.requests.project.AddFieldRequest;
import com.devteam.aiauditserver.requests.project.AddStepRequest;
import com.devteam.aiauditserver.requests.project.AssignAuditRequest;
import com.devteam.aiauditserver.requests.project.CreateAuditFormTemplateRequest;
import com.devteam.aiauditserver.responses.Response.DynamicResponse;

import com.devteam.aiauditserver.services.auth.UserService;
import com.devteam.aiauditserver.services.project.audit.AuditFormTemplateService;
import com.devteam.aiauditserver.services.project.audit.AuditRequestService;
import com.devteam.aiauditserver.services.project.audit.DefaultTemplateLoaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/v1/admin/audits")
public class AdminAuditsController extends BaseController {

    @Autowired
    private AuditFormTemplateService templateService;

    @Autowired
    private DefaultTemplateLoaderService defaultTemplateLoaderService;

    @Autowired
    private AuditRequestService requestService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuditFormStepRepository stepRepository;

    @Autowired
    private AuditFormFieldRepository fieldRepository;

    @Autowired
    private AuditFormTemplateRepository templateRepository;

    @Autowired
    private AuditProcessStepRepository processStepRepository;

    // ════════════════════════════════════════
    // FORM TEMPLATE MANAGEMENT
    // ════════════════════════════════════════

    @GetMapping("/templates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditFormTemplate>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }

    @GetMapping("/templates/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditFormTemplate> getTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(templateService.getById(id));
    }

    @GetMapping("/templates/type/{auditType}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditFormTemplate> getTemplateByType(
            @PathVariable AuditType auditType) {
        return ResponseEntity.ok(templateService.getByAuditType(auditType));
    }

    @PostMapping("/templates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createTemplate(
            @RequestBody CreateAuditFormTemplateRequest req) {
        try {
            AuditFormTemplate created = templateService.createTemplate(req);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/templates/generate-default-rics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> generateDefaultRicsTemplate() {
        try {
            AuditFormTemplate created =
                    defaultTemplateLoaderService.generateDefaultRicsTemplate();
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.CONFLICT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PatchMapping("/templates/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditFormTemplate> toggleTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(templateService.toggleActive(id));
    }

    @PatchMapping("/templates/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditFormTemplate> updateTemplate(
            @PathVariable Long id,
            @RequestBody CreateAuditFormTemplateRequest req) {
        return ResponseEntity.ok(templateService.updateTemplate(id, req));
    }

    // ── Steps

    @PostMapping("/templates/{templateId}/steps")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditFormStep> addStep(
            @PathVariable Long templateId,
            @RequestBody AddStepRequest req) {
        return new ResponseEntity<>(
                templateService.addStep(templateId, req), HttpStatus.CREATED);
    }

    @DeleteMapping("/steps/{stepId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeStep(@PathVariable Long stepId) {
        templateService.removeStep(stepId);
        return ResponseEntity.noContent().build();
    }

    // ── Fields

    @PostMapping("/steps/{stepId}/fields")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditFormField> addField(
            @PathVariable Long stepId,
            @RequestBody AddFieldRequest req) {
        return new ResponseEntity<>(
                templateService.addField(stepId, req), HttpStatus.CREATED);
    }

    @DeleteMapping("/fields/{fieldId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeField(@PathVariable Long fieldId) {
        templateService.removeField(fieldId);
        return ResponseEntity.noContent().build();
    }

    // ════════════════════════════════════════
    // AUDIT REQUEST MANAGEMENT
    // ════════════════════════════════════════

    @GetMapping("/requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DynamicResponse> getAllRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) AuditStatus status,
            @RequestParam(required = false) String search) {

        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(
                    requestService.searchRequests(search, page, size));
        }
        return ResponseEntity.ok(
                requestService.getAllRequests(page, size, status));
    }

    @GetMapping("/requests/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditRequest> getRequest(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.getById(id));
    }

    @PatchMapping("/requests/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditRequest> assignRequest(
            @PathVariable Long id,
            @RequestBody AssignAuditRequest req) {
        return ResponseEntity.ok(requestService.assignRequest(id, req));
    }

    @PatchMapping("/requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditRequest> rejectRequest(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(
                requestService.rejectRequest(id, reason != null ? reason : ""));
    }

    @PatchMapping("/requests/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditRequest> completeRequest(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.completeAudit(id));
    }

    // Admin's own assigned audits
    @GetMapping("/my-audits")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DynamicResponse> getMyAudits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long adminId = userService.findByUserName(
                getCurrentUser().getUsername()).getId();
        return ResponseEntity.ok(
                requestService.getRequestsAssignedTo(adminId, page, size));
    }

    @PatchMapping("/requests/{id}/start")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditRequest> startAudit(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.startAudit(id));
    }

    // Reorder steps
    @PatchMapping("/templates/{templateId}/steps/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reorderSteps(
            @PathVariable Long templateId,
            @RequestBody List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            Long stepId   = Long.valueOf(item.get("id").toString());
            Integer order = Integer.valueOf(item.get("stepOrder").toString());
            AuditFormStep step = stepRepository.findById(stepId).orElse(null);
            if (step != null) {
                step.setStepOrder(order);
                stepRepository.save(step);
            }
        }
        return ResponseEntity.noContent().build();
    }

    // Reorder fields
    @PatchMapping("/steps/{stepId}/fields/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reorderFields(
            @PathVariable Long stepId,
            @RequestBody List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            Long fieldId  = Long.valueOf(item.get("id").toString());
            Integer order = Integer.valueOf(item.get("fieldOrder").toString());
            AuditFormField field = fieldRepository.findById(fieldId).orElse(null);
            if (field != null) {
                field.setFieldOrder(order);
                fieldRepository.save(field);
            }
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/process-steps/{auditType}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditProcessStep>> getProcessStepsForAuditType(
            @PathVariable AuditType auditType) {
        return templateRepository.findByAuditType(auditType)
                .map(template -> ResponseEntity.ok(
                        processStepRepository.findByTemplateIdOrderByStepOrderAsc(template.getId())
                ))
                .orElse(ResponseEntity.ok(List.of()));
    }
}