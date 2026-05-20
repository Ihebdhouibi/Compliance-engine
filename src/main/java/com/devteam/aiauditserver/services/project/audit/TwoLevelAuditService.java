package com.devteam.aiauditserver.services.project.audit;

import com.devteam.aiauditserver.enums.Project.AuditLevel;
import com.devteam.aiauditserver.enums.Project.AuditPhase;
import com.devteam.aiauditserver.enums.Project.AuditStatus;
import com.devteam.aiauditserver.enums.Project.AuditType;
import com.devteam.aiauditserver.models.User.User;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormField;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormStep;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormTemplate;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditAnswerScoring;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequest;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequestAnswer;
import com.devteam.aiauditserver.models.project.AuditRequest.RoutingProfile;
import com.devteam.aiauditserver.repositories.project.AuditAnswerScoringRepository;
import com.devteam.aiauditserver.repositories.project.AuditFormTemplateRepository;
import com.devteam.aiauditserver.repositories.project.AuditRequestAnswerRepository;
import com.devteam.aiauditserver.repositories.project.AuditRequestRepository;
import com.devteam.aiauditserver.repositories.project.RoutingProfileRepository;
import com.devteam.aiauditserver.requests.project.ScoreAnswerRequest;
import com.devteam.aiauditserver.requests.project.SubmitTwoLevelRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service layer for the two-level audit flow.
 *
 * Flow:
 *   1. {@link #getLevel1Form()}           — returns the active L1 template.
 *   2. {@link #submitLevel1}              — persists answers, computes a
 *                                            RoutingProfile, advances phase to ROUTED.
 *   3. {@link #confirmQuote}              — phase → QUOTE_ACCEPTED, links the
 *                                            chosen L2 template.
 *   4. {@link #getLevel2Form}             — returns the L2 template filtered
 *                                            to the modules picked by routing.
 *   5. {@link #submitLevel2}              — appends L2 answers, phase → L2_SUBMITTED.
 *   6. {@link #scoreAnswer}               — auditor maturity score per answer.
 */
@Service
public class TwoLevelAuditService {

    @Autowired private AuditFormTemplateRepository templateRepo;
    @Autowired private AuditRequestRepository requestRepo;
    @Autowired private AuditRequestAnswerRepository answerRepo;
    @Autowired private AuditAnswerScoringRepository scoringRepo;
    @Autowired private RoutingProfileRepository routingProfileRepo;
    @Autowired private RoutingEngine routingEngine;

    // ── L1 ──────────────────────────────────────────────────────────────

    public AuditFormTemplate getLevel1Form() {
        return templateRepo
                .findFirstByLevelAndActiveTrueOrderByTemplateVersionDesc(AuditLevel.LEVEL_1)
                .orElseThrow(() -> new RuntimeException("No Level 1 template seeded yet"));
    }

    @Transactional
    public AuditRequest submitLevel1(User submittedBy, SubmitTwoLevelRequest req) {
        AuditFormTemplate l1 = getLevel1Form();

        AuditRequest auditRequest = new AuditRequest();
        // AuditType is the catch-all bucket for L1; L2 will use the routed type.
        auditRequest.setAuditType(AuditType.AI_READINESS_REVIEW);
        auditRequest.setSubmittedBy(submittedBy);
        auditRequest.setStatus(AuditStatus.SUBMITTED);
        auditRequest.setPhase(AuditPhase.L1_SUBMITTED);
        auditRequest.setLevel1TemplateId(l1.getId());

        if (req.getAnswers() != null) {
            for (SubmitTwoLevelRequest.TwoLevelAnswer a : req.getAnswers()) {
                AuditRequestAnswer ans = new AuditRequestAnswer();
                ans.setFieldId(a.getFieldId());
                ans.setFieldKey(a.getFieldKey());
                ans.setFieldLabel(a.getFieldLabel());
                ans.setAnswerValue(a.getAnswerValue());
                ans.setAuditRequest(auditRequest);
                auditRequest.getAnswers().add(ans);
            }
        }
        AuditRequest saved = requestRepo.save(auditRequest);

        // Run routing engine on the saved answers
        RoutingProfile profile = routingEngine.computeFor(saved);
        saved.setPhase(AuditPhase.ROUTED);
        return requestRepo.save(saved);
    }

    // ── routing & quote ────────────────────────────────────────────────

    public RoutingProfile getRoutingProfile(Long requestId) {
        return routingProfileRepo.findByAuditRequestId(requestId)
                .orElseThrow(() -> new RuntimeException(
                        "No routing profile for request " + requestId));
    }

    @Transactional
    public AuditRequest confirmQuote(Long requestId) {
        AuditRequest req = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        RoutingProfile profile = getRoutingProfile(requestId);

        // Always route to the newest ACTIVE RICS L2 template (skips legacy
        // non-versioned templates seeded by DefaultTemplateLoaderService).
        AuditFormTemplate l2 = templateRepo.findFirstByAuditTypeAndLevelAndActiveTrueOrderByTemplateVersionDescIdDesc(
                        AuditType.RICS_RESPONSIBLE_AI, AuditLevel.LEVEL_2)
                .orElseThrow(() -> new RuntimeException(
                        "RICS Level 2 template not seeded"));
        req.setLevel2TemplateId(l2.getId());
        req.setAuditType(AuditType.RICS_RESPONSIBLE_AI);
        req.setPhase(AuditPhase.QUOTE_ACCEPTED);
        profile.setConfirmedAt(new Date());
        routingProfileRepo.save(profile);
        return requestRepo.save(req);
    }

    // ── L2 ──────────────────────────────────────────────────────────────

    /**
     * Returns the L2 template filtered to only the modules active for this
     * audit (per RoutingProfile.activeModules). Steps with no remaining
     * fields are removed.
     */
    public AuditFormTemplate getLevel2Form(Long requestId) {
        AuditRequest req = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        Long tplId = req.getLevel2TemplateId();
        AuditFormTemplate l2 = tplId != null
                ? templateRepo.findById(tplId).orElse(null)
                : null;
        // If the pinned template is missing or has been deactivated (e.g. it
        // was a legacy/broken template replaced by the seeder), fall back to
        // the newest active RICS L2 template.
        if (l2 == null || !Boolean.TRUE.equals(l2.getActive())) {
            l2 = templateRepo.findFirstByAuditTypeAndLevelAndActiveTrueOrderByTemplateVersionDescIdDesc(
                    AuditType.RICS_RESPONSIBLE_AI, AuditLevel.LEVEL_2).orElse(null);
        }
        if (l2 == null) {
            throw new RuntimeException("No Level 2 template available");
        }

        Set<String> activeModules = routingProfileRepo.findByAuditRequestId(requestId)
                .map(p -> p.getActiveModules() == null ? null
                        : Arrays.stream(p.getActiveModules().split(","))
                              .map(String::trim).collect(Collectors.toSet()))
                .orElse(null);
        if (activeModules == null || activeModules.isEmpty()) {
            return l2; // no filter
        }

        // Filter in-place on a transient copy by detaching the underlying lists.
        // Because the entity is loaded inside a Hibernate session, we must not
        // mutate the original. Return a detached DTO-shaped projection by
        // editing a copy via the entity setters on a fresh instance.
        AuditFormTemplate copy = new AuditFormTemplate();
        copy.setId(l2.getId());
        copy.setAuditType(l2.getAuditType());
        copy.setLevel(l2.getLevel());
        copy.setTemplateVersion(l2.getTemplateVersion());
        copy.setTitle(l2.getTitle());
        copy.setDescription(l2.getDescription());
        copy.setActive(l2.getActive());
        copy.setCreatedAt(l2.getCreatedAt());

        for (AuditFormStep step : l2.getSteps()) {
            List<AuditFormField> kept = step.getFields().stream()
                    .filter(f -> f.getModule() == null || activeModules.contains(f.getModule()))
                    .collect(Collectors.toList());
            if (kept.isEmpty()) continue;
            AuditFormStep newStep = new AuditFormStep();
            newStep.setId(step.getId());
            newStep.setStepOrder(step.getStepOrder());
            newStep.setTitle(step.getTitle());
            newStep.setDescription(step.getDescription());
            newStep.setFields(kept);
            copy.getSteps().add(newStep);
        }
        return copy;
    }

    @Transactional
    public AuditRequest submitLevel2(Long requestId, SubmitTwoLevelRequest req) {
        AuditRequest auditRequest = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (req.getAnswers() != null) {
            for (SubmitTwoLevelRequest.TwoLevelAnswer a : req.getAnswers()) {
                AuditRequestAnswer existing = null;
                if (a.getFieldId() != null) {
                    existing = answerRepo
                            .findByAuditRequestIdAndFieldId(requestId, a.getFieldId())
                            .orElse(null);
                }
                if (existing == null) {
                    existing = new AuditRequestAnswer();
                    existing.setFieldId(a.getFieldId());
                    existing.setAuditRequest(auditRequest);
                    auditRequest.getAnswers().add(existing);
                }
                existing.setFieldKey(a.getFieldKey());
                if (a.getFieldLabel() != null) existing.setFieldLabel(a.getFieldLabel());
                existing.setAnswerValue(a.getAnswerValue());
            }
        }
        auditRequest.setPhase(AuditPhase.L2_SUBMITTED);
        auditRequest.setStatus(AuditStatus.SUBMITTED);
        return requestRepo.save(auditRequest);
    }

    // ── auditor scoring ─────────────────────────────────────────────────

    @Transactional
    public AuditAnswerScoring scoreAnswer(Long answerId, ScoreAnswerRequest req, User scoredBy) {
        AuditRequestAnswer answer = answerRepo.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Answer not found"));
        AuditAnswerScoring scoring = scoringRepo.findByAnswerId(answerId)
                .orElseGet(AuditAnswerScoring::new);
        scoring.setAnswer(answer);
        scoring.setAuditorScore(req.getAuditorScore());
        scoring.setAuditorNotes(req.getAuditorNotes());
        scoring.setEvidenceProvidedStatus(req.getEvidenceProvidedStatus());
        scoring.setEvidenceReference(req.getEvidenceReference());
        scoring.setScoredBy(scoredBy);
        scoring.setScoredAt(new Date());
        return scoringRepo.save(scoring);
    }

    public List<AuditAnswerScoring> getScorings(Long requestId) {
        return scoringRepo.findAllByAuditRequestId(requestId);
    }
}
