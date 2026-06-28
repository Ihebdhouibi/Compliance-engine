package com.devteam.aiauditserver.services.project.audit;

import com.devteam.aiauditserver.Tools.util.StepMetaParser;
import com.devteam.aiauditserver.models.User.User;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditJournalEntry;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditJournalEntry.Action;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditJournalEntry.ChangeType;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequest;
import com.devteam.aiauditserver.repositories.project.AuditJournalEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Computes and persists field-level change journal entries when an
 * already-COMPLETED audit's step result is edited.
 */
@Service
public class AuditJournalService {

    @Autowired private AuditJournalEntryRepository journalRepo;

    public List<AuditJournalEntry> getJournal(Long auditRequestId) {
        return journalRepo.findByAuditRequestIdOrderByChangedAtDesc(auditRequestId);
    }

    /**
     * Diff the old vs new step-result descriptions and record one journal entry
     * per changed verdict / finding / recommendation / note. No-op when nothing
     * actually changed.
     */
    public void recordStepResultDiff(AuditRequest request, String stepName,
                                     String oldDescription, String newDescription,
                                     User changedBy) {
        StepMetaParser.ParsedStep before = StepMetaParser.parse(oldDescription);
        StepMetaParser.ParsedStep after  = StepMetaParser.parse(newDescription);

        List<AuditJournalEntry> entries = new ArrayList<>();

        diffMap(entries, request, stepName, changedBy, ChangeType.VERDICT,
                before.verdicts, after.verdicts);
        diffMap(entries, request, stepName, changedBy, ChangeType.FINDING,
                before.findings, after.findings);
        diffMap(entries, request, stepName, changedBy, ChangeType.RECOMMENDATION,
                before.recommendations, after.recommendations);

        if (!Objects.equals(before.note, after.note)) {
            entries.add(new AuditJournalEntry(request, changedBy, stepName,
                    ChangeType.NOTE, actionFor(before.note, after.note),
                    "note", nullIfBlank(before.note), nullIfBlank(after.note)));
        }

        if (!entries.isEmpty()) {
            journalRepo.saveAll(entries);
        }
    }

    private void diffMap(List<AuditJournalEntry> entries, AuditRequest request,
                         String stepName, User changedBy, ChangeType type,
                         Map<String, String> before, Map<String, String> after) {
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(before.keySet());
        keys.addAll(after.keySet());

        for (String key : keys) {
            String oldVal = before.get(key);
            String newVal = after.get(key);
            if (Objects.equals(oldVal, newVal)) continue;
            entries.add(new AuditJournalEntry(request, changedBy, stepName, type,
                    actionFor(oldVal, newVal), key, oldVal, newVal));
        }
    }

    private Action actionFor(String oldVal, String newVal) {
        boolean hadOld = oldVal != null && !oldVal.isBlank();
        boolean hasNew = newVal != null && !newVal.isBlank();
        if (!hadOld && hasNew) return Action.ADDED;
        if (hadOld && !hasNew) return Action.REMOVED;
        return Action.MODIFIED;
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
