package com.devteam.aiauditserver.services.project.audit;


import com.devteam.aiauditserver.enums.Project.AuditStatus;
import com.devteam.aiauditserver.models.File.MediaModel;
import com.devteam.aiauditserver.models.User.User;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormField;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequest;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequestAnswer;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequestAnswerFile;
import com.devteam.aiauditserver.repositories.File.FilesStorageService;
import com.devteam.aiauditserver.repositories.project.AuditFormFieldRepository;
import com.devteam.aiauditserver.repositories.project.AuditRequestAnswerFileRepository;
import com.devteam.aiauditserver.repositories.project.AuditRequestAnswerRepository;
import com.devteam.aiauditserver.repositories.project.AuditRequestRepository;
import com.devteam.aiauditserver.requests.project.AssignAuditRequest;
import com.devteam.aiauditserver.requests.project.SubmitAuditRequest;
import com.devteam.aiauditserver.responses.Response.DynamicResponse;
import com.devteam.aiauditserver.services.auth.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

@Service
public class AuditRequestService {

    @Autowired
    private AuditRequestRepository requestRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AuditRequestAnswerRepository answerRepository;

    @Autowired
    private AuditRequestAnswerFileRepository answerFileRepository;

    @Autowired
    private AuditFormFieldRepository fieldRepository;

    @Autowired
    private FilesStorageService filesStorageService;

    @Autowired(required = false)
    private com.devteam.aiauditserver.services.project.ocr.OcrOrchestratorService ocrOrchestrator;


    @Transactional
    public AuditRequest uploadAnswerFile(Long requestId,
                                         Long fieldId,
                                         MultipartFile file) {

        AuditRequest auditRequest = getById(requestId);

        AuditFormField field = fieldRepository.findById(fieldId).orElse(null);
        boolean multi = field != null
                && Boolean.TRUE.equals(field.getMultipleFiles());
        String fieldLabel = field != null ? field.getLabel() : "File Upload";

        // Find existing answer for this field or create a new one
        AuditRequestAnswer answer = answerRepository
                .findByAuditRequestIdAndFieldId(requestId, fieldId)
                .orElseGet(() -> {
                    AuditRequestAnswer a = new AuditRequestAnswer();
                    a.setFieldId(fieldId);
                    a.setFieldLabel(fieldLabel);
                    a.setAuditRequest(auditRequest);
                    return a;
                });

        MediaModel saved = filesStorageService.save_file(
                file, "audits/answers/" + requestId);

        if (multi) {
            int order = answer.getFiles() != null ? answer.getFiles().size() : 0;
            AuditRequestAnswerFile entry = new AuditRequestAnswerFile(saved, answer, order);
            answer.getFiles().add(entry);
        } else {
            answer.setFileMedia(saved);
        }
        answerRepository.save(answer);

        // Return the refreshed request with answers
        return getById(requestId);
    }

    @Transactional
    public AuditRequest uploadAnswerFiles(Long requestId,
                                          Long fieldId,
                                          MultipartFile[] files) {
        if (files != null) {
            for (MultipartFile f : files) {
                if (f != null && !f.isEmpty()) {
                    uploadAnswerFile(requestId, fieldId, f);
                }
            }
        }
        return getById(requestId);
    }

    @Transactional
    public AuditRequest removeAnswerFile(Long requestId, Long fieldId, Long fileId) {
        AuditRequestAnswer answer = answerRepository
                .findByAuditRequestIdAndFieldId(requestId, fieldId)
                .orElseThrow(() -> new RuntimeException("Answer not found"));

        AuditRequestAnswerFile target = answer.getFiles().stream()
                .filter(f -> f.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("File not found in answer"));

        answer.getFiles().remove(target);
        answerFileRepository.delete(target);
        answerRepository.save(answer);
        return getById(requestId);
    }

    // ── User submits an audit request
    public AuditRequest submitRequest(User submittedBy, SubmitAuditRequest req) {
        AuditRequest auditRequest = new AuditRequest();
        auditRequest.setAuditType(req.getAuditType());
        auditRequest.setSubmittedBy(submittedBy);
        auditRequest.setStatus(AuditStatus.SUBMITTED);

        if (req.getAnswers() != null) {
            for (SubmitAuditRequest.AnswerRequest a : req.getAnswers()) {
                AuditRequestAnswer answer = new AuditRequestAnswer();
                answer.setFieldId(a.getFieldId());
                answer.setFieldLabel(a.getFieldLabel());
                answer.setAnswerValue(a.getAnswerValue());
                answer.setAuditRequest(auditRequest);
                auditRequest.getAnswers().add(answer);
            }
        }

        AuditRequest saved = requestRepository.save(auditRequest);

        // Fire-and-forget OCR on all evidence attached to this request.
        // Runs on a separate thread (@Async) so the HTTP submit returns fast.
        if (ocrOrchestrator != null) {
            try { ocrOrchestrator.submitEvidenceForOcr(saved); }
            catch (Exception ignored) { /* never block submit on OCR */ }
        }
        return saved;
    }

    // ── Admin assigns audit to an auditor (or themselves)
    public AuditRequest assignRequest(Long requestId, AssignAuditRequest req) {
        AuditRequest auditRequest = getById(requestId);

        User assignedTo = userService.findById(req.getAssignedToUserId());
        if (assignedTo == null) {
            throw new RuntimeException("Assigned user not found");
        }

        auditRequest.setAssignedTo(assignedTo);
        auditRequest.setStatus(AuditStatus.ASSIGNED);
        auditRequest.setAssignedAt(new Date());

        if (req.getDueDate() != null) {
            auditRequest.setDueDate(req.getDueDate());
        }

        return requestRepository.save(auditRequest);
    }

    // ── Auditor starts working on audit
    public AuditRequest startAudit(Long requestId) {
        AuditRequest auditRequest = getById(requestId);
        auditRequest.setStatus(AuditStatus.IN_PROGRESS);
        return requestRepository.save(auditRequest);
    }

    // ── Auditor/Admin completes audit
    public AuditRequest completeAudit(Long requestId) {
        AuditRequest auditRequest = getById(requestId);
        auditRequest.setStatus(AuditStatus.COMPLETED);
        auditRequest.setCompletedAt(new Date());
        return requestRepository.save(auditRequest);
    }

    // ── Admin rejects audit request
    public AuditRequest rejectRequest(Long requestId, String reason) {
        AuditRequest auditRequest = getById(requestId);
        auditRequest.setStatus(AuditStatus.REJECTED);
        auditRequest.setRejectionReason(reason);
        return requestRepository.save(auditRequest);
    }

    // ── Admin: get all requests paginated
    public DynamicResponse getAllRequests(int page, int size, AuditStatus status) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditRequest> result;

        if (status != null) {
            result = requestRepository.findByStatusOrderBySubmittedAtDesc(status, pageable);
        } else {
            result = requestRepository.findAllByOrderBySubmittedAtDesc(pageable);
        }

        return new DynamicResponse(
                result.getContent(),
                result.getNumber(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    // ── Admin: search requests
    public DynamicResponse searchRequests(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditRequest> result = requestRepository.searchRequests(search, pageable);
        return new DynamicResponse(
                result.getContent(),
                result.getNumber(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    // ── Admin: requests assigned to a specific user
    public DynamicResponse getRequestsAssignedTo(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditRequest> result =
                requestRepository.findByAssignedToIdOrderBySubmittedAtDesc(userId, pageable);
        return new DynamicResponse(
                result.getContent(),
                result.getNumber(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    // ── User: get my submitted requests
    public DynamicResponse getMyRequests(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditRequest> result =
                requestRepository.findBySubmittedByIdOrderBySubmittedAtDesc(userId, pageable);
        return new DynamicResponse(
                result.getContent(),
                result.getNumber(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    // ── Auditor: get audits assigned to me
    public DynamicResponse getMyAssignedAudits(Long auditorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditRequest> result =
                requestRepository.findByAssignedToIdOrderBySubmittedAtDesc(auditorId, pageable);
        return new DynamicResponse(
                result.getContent(),
                result.getNumber(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    // ── Get single request by id
    public AuditRequest getById(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Audit request not found: " + id));
    }

    // ── Dashboard stats
    public long countByStatus(AuditStatus status) {
        return requestRepository.countByStatus(status);
    }
}