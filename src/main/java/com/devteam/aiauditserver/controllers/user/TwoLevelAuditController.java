package com.devteam.aiauditserver.controllers.user;

import com.devteam.aiauditserver.Tools.util.BaseController;
import com.devteam.aiauditserver.models.User.User;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormOptionList;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormTemplate;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditAnswerScoring;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequest;
import com.devteam.aiauditserver.models.project.AuditRequest.RoutingProfile;
import com.devteam.aiauditserver.requests.project.ScoreAnswerRequest;
import com.devteam.aiauditserver.requests.project.SubmitTwoLevelRequest;
import com.devteam.aiauditserver.services.auth.UserService;
import com.devteam.aiauditserver.services.project.audit.OptionListService;
import com.devteam.aiauditserver.services.project.audit.TwoLevelAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Two-level audit endpoints. Mounted under {@code /api/v1/two-level/...} so
 * the existing single-level controllers stay untouched. Frontends decide
 * whether to use the legacy or two-level flow on a per-audit-type basis.
 */
@RestController
@CrossOrigin
@RequestMapping("/api/v1/two-level")
public class TwoLevelAuditController extends BaseController {

    @Autowired private TwoLevelAuditService twoLevelService;
    @Autowired private OptionListService optionListService;
    @Autowired private UserService userService;

    // ── Option lists (shared) ───────────────────────────────────────────

    @GetMapping("/option-lists")
    @PreAuthorize("hasAnyRole('USER','ADMIN','AUDITOR')")
    public ResponseEntity<List<AuditFormOptionList>> listOptionLists() {
        return ResponseEntity.ok(optionListService.getAll());
    }

    @GetMapping("/option-lists/{key}")
    @PreAuthorize("hasAnyRole('USER','ADMIN','AUDITOR')")
    public ResponseEntity<Map<String, Object>> getOptionList(@PathVariable String key) {
        Map<String, Object> body = optionListService.getByKey(key);
        if (body == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(body);
    }

    // ── Level 1 ─────────────────────────────────────────────────────────

    @GetMapping("/level1/form")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AuditFormTemplate> getLevel1Form() {
        return ResponseEntity.ok(twoLevelService.getLevel1Form());
    }

    @PostMapping("/level1/submit")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AuditRequest> submitLevel1(@RequestBody SubmitTwoLevelRequest req) {
        User me = userService.findByUserName(getCurrentUser().getUsername());
        AuditRequest created = twoLevelService.submitLevel1(me, req);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // ── Routing profile / quote ─────────────────────────────────────────

    @GetMapping("/requests/{id}/routing-profile")
    @PreAuthorize("hasAnyRole('USER','ADMIN','AUDITOR')")
    public ResponseEntity<RoutingProfile> getRoutingProfile(@PathVariable Long id) {
        return ResponseEntity.ok(twoLevelService.getRoutingProfile(id));
    }

    @PatchMapping("/requests/{id}/confirm-quote")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AuditRequest> confirmQuote(@PathVariable Long id) {
        return ResponseEntity.ok(twoLevelService.confirmQuote(id));
    }

    // ── Level 2 ─────────────────────────────────────────────────────────

    @GetMapping("/requests/{id}/level2/form")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AuditFormTemplate> getLevel2Form(@PathVariable Long id) {
        return ResponseEntity.ok(twoLevelService.getLevel2Form(id));
    }

    @PostMapping("/requests/{id}/level2/submit")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AuditRequest> submitLevel2(@PathVariable Long id,
                                                     @RequestBody SubmitTwoLevelRequest req) {
        return ResponseEntity.ok(twoLevelService.submitLevel2(id, req));
    }

    // ── Auditor scoring ─────────────────────────────────────────────────

    @PutMapping("/answers/{answerId}/score")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public ResponseEntity<AuditAnswerScoring> scoreAnswer(@PathVariable Long answerId,
                                                          @RequestBody ScoreAnswerRequest req) {
        User me = userService.findByUserName(getCurrentUser().getUsername());
        return ResponseEntity.ok(twoLevelService.scoreAnswer(answerId, req, me));
    }

    @GetMapping("/requests/{id}/scorings")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR','USER')")
    public ResponseEntity<List<AuditAnswerScoring>> getScorings(@PathVariable Long id) {
        return ResponseEntity.ok(twoLevelService.getScorings(id));
    }
}
