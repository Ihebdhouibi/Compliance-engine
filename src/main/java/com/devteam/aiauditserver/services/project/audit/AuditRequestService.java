package com.devteam.aiauditserver.services.project.audit;


import com.devteam.aiauditserver.enums.Project.AuditStatus;
import com.devteam.aiauditserver.models.File.MediaModel;
import com.devteam.aiauditserver.models.User.User;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequest;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequestAnswer;
import com.devteam.aiauditserver.repositories.File.FilesStorageService;
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
    private FilesStorageService filesStorageService;


    @Transactional
    public AuditRequest uploadAnswerFile(Long requestId,
                                         Long fieldId,
                                         MultipartFile file) {

        AuditRequest auditRequest = getById(requestId);

        // Find existing answer for this field or create a new one
        AuditRequestAnswer answer = answerRepository
                .findByAuditRequestIdAndFieldId(requestId, fieldId)
                .orElseGet(() -> {
                    AuditRequestAnswer a = new AuditRequestAnswer();
                    a.setFieldId(fieldId);
                    a.setFieldLabel("File Upload");   // overwritten below if found
                    a.setAuditRequest(auditRequest);
                    return a;
                });

        // Save the file and get a MediaModel back — same pattern as profile image
        MediaModel saved = filesStorageService.save_file(
                file, "audits/answers/" + requestId);

        answer.setFileMedia(saved);
        answerRepository.save(answer);

        // Return the refreshed request with answers
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

        return requestRepository.save(auditRequest);
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