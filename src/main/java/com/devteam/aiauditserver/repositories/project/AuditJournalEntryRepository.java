package com.devteam.aiauditserver.repositories.project;

import com.devteam.aiauditserver.models.project.AuditRequest.AuditJournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditJournalEntryRepository extends JpaRepository<AuditJournalEntry, Long> {
    List<AuditJournalEntry> findByAuditRequestIdOrderByChangedAtDesc(Long auditRequestId);
}
