package com.devteam.aiauditserver.services.project.audit;


import com.devteam.aiauditserver.models.User.User;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditProcessStep;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequest;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditStepResult;
import com.devteam.aiauditserver.repositories.project.AuditProcessStepRepository;
import com.devteam.aiauditserver.repositories.project.AuditRequestRepository;
import com.devteam.aiauditserver.repositories.project.AuditStepResultRepository;
import com.devteam.aiauditserver.requests.project.SaveStepResultRequest;
import com.devteam.aiauditserver.services.auth.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AuditStepResultService {

    @Autowired private AuditStepResultRepository resultRepo;
    @Autowired private AuditProcessStepRepository stepRepo;
    @Autowired private AuditRequestRepository requestRepo;
    @Autowired private UserService userService;

    public List<AuditStepResult> getResultsForRequest(Long requestId) {
        return resultRepo.findByAuditRequestIdOrderByCreatedAtAsc(requestId);
    }

    // Upsert: creates new result or updates existing one for same request+step
    public AuditStepResult saveOrUpdate(Long requestId,
                                        SaveStepResultRequest req,
                                        String username) {
        AuditRequest auditRequest = requestRepo.findById(requestId)
                .orElseThrow(() ->
                        new RuntimeException("AuditRequest not found: " + requestId));

        User user = userService.findByUserName(username);

        // Resolve process step — null means default virtual step
        AuditProcessStep processStep = null;
        if (req.getProcessStepId() != null && req.getProcessStepId() > 0) {
            processStep = stepRepo.findById(req.getProcessStepId()).orElse(null);
        }

        // Always snapshot the name for historical preservation
        String stepName = processStep != null
                ? processStep.getName()
                : (req.getStepName() != null
                ? req.getStepName() : "Audit Review");

        // Try to find existing result for this request + step combination
        Optional<AuditStepResult> existing = processStep != null
                ? resultRepo.findByAuditRequestIdAndProcessStepId(
                requestId, processStep.getId())
                : Optional.empty();

        AuditStepResult result = existing.orElseGet(AuditStepResult::new);
        result.setAuditRequest(auditRequest);
        result.setProcessStep(processStep);
        result.setStepName(stepName);
        result.setDescription(req.getDescription());
        result.setFilledBy(user);
        result.setStatus(parseStatus(req.getStatus()));

        return resultRepo.save(result);
    }

    public AuditStepResult updateResult(Long resultId,
                                        SaveStepResultRequest req) {
        AuditStepResult result = resultRepo.findById(resultId)
                .orElseThrow(() ->
                        new RuntimeException("StepResult not found: " + resultId));
        result.setDescription(req.getDescription());
        result.setStatus(parseStatus(req.getStatus()));
        return resultRepo.save(result);
    }

    public void deleteResult(Long resultId) {
        resultRepo.deleteById(resultId);
    }

    private AuditStepResult.StepResultStatus parseStatus(String s) {
        if ("SAVED".equalsIgnoreCase(s))
            return AuditStepResult.StepResultStatus.SAVED;
        return AuditStepResult.StepResultStatus.DRAFT;
    }
}
