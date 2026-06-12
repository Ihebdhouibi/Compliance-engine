package com.devteam.aiauditserver.controllers.user;


import com.devteam.aiauditserver.Tools.util.BaseController;
import com.devteam.aiauditserver.enums.Project.AuditType;
import com.devteam.aiauditserver.models.User.User;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormTemplate;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequest;
import com.devteam.aiauditserver.requests.project.SubmitAuditRequest;
import com.devteam.aiauditserver.responses.Response.DynamicResponse;
import com.devteam.aiauditserver.services.auth.UserService;
import com.devteam.aiauditserver.services.project.audit.AuditFormTemplateService;
import com.devteam.aiauditserver.services.project.audit.AuditRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@CrossOrigin
@RequestMapping("/api/v1/user/audits")
public class UserAuditController extends BaseController {

    @Autowired
    private AuditFormTemplateService templateService;

    @Autowired
    private AuditRequestService requestService;

    @Autowired
    private UserService userService;


    // ── Get the audit form for a specific type (to render the form)
    @GetMapping("/form/{auditType}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AuditFormTemplate> getAuditForm(
            @PathVariable AuditType auditType) {
        return ResponseEntity.ok(templateService.getByAuditType(auditType));
    }

    // ── Submit a completed audit request
    @PostMapping("/submit")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AuditRequest> submitAuditRequest(
            @RequestBody SubmitAuditRequest req) {

        User me = userService.findByUserName(getCurrentUser().getUsername());
        AuditRequest created = requestService.submitRequest(me, req);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // ── Get my audit requests with pagination
    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<DynamicResponse> getMyRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User me = userService.findByUserName(getCurrentUser().getUsername());
        return ResponseEntity.ok(
                requestService.getMyRequests(me.getId(), page, size));
    }

    // ── Get single request detail
    @GetMapping("/my-requests/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AuditRequest> getMyRequest(@PathVariable Long id) {
        AuditRequest request = requestService.getById(id);
        User me = userService.findByUserName(getCurrentUser().getUsername());

        // Security: user can only see their own requests
        if (!request.getSubmittedBy().getId().equals(me.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(request);
    }

    @PatchMapping("/my-requests/{requestId}/upload/{fieldId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> uploadAnswerFile(
            @PathVariable Long requestId,
            @PathVariable Long fieldId,
            @RequestParam MultipartFile file) {

        // Security: only the owner can upload files to their own request
        User me = userService.findByUserName(getCurrentUser().getUsername());
        AuditRequest request = requestService.getById(requestId);

        if (!request.getSubmittedBy().getId().equals(me.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You can only upload files to your own requests");
        }

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is required");
        }

        AuditRequest updated = requestService.uploadAnswerFile(requestId, fieldId, file);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/my-requests/{requestId}/upload-multi/{fieldId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> uploadAnswerFiles(
            @PathVariable Long requestId,
            @PathVariable Long fieldId,
            @RequestParam("files") MultipartFile[] files) {

        User me = userService.findByUserName(getCurrentUser().getUsername());
        AuditRequest request = requestService.getById(requestId);

        if (!request.getSubmittedBy().getId().equals(me.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You can only upload files to your own requests");
        }

        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body("At least one file is required");
        }

        AuditRequest updated = requestService.uploadAnswerFiles(requestId, fieldId, files);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/my-requests/{requestId}/upload/{fieldId}/file/{fileId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> removeAnswerFile(
            @PathVariable Long requestId,
            @PathVariable Long fieldId,
            @PathVariable Long fileId) {

        User me = userService.findByUserName(getCurrentUser().getUsername());
        AuditRequest request = requestService.getById(requestId);

        if (!request.getSubmittedBy().getId().equals(me.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        AuditRequest updated = requestService.removeAnswerFile(requestId, fieldId, fileId);
        return ResponseEntity.ok(updated);
    }
}
