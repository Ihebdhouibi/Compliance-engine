package com.devteam.aiauditserver.repositories.project;

import com.devteam.aiauditserver.models.project.AuditRequest.AuditStepResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditStepResultRepository extends JpaRepository<AuditStepResult, Long> {
    List<AuditStepResult> findByAuditRequestIdOrderByCreatedAtAsc(Long requestId);
    List<AuditStepResult> findByAuditRequestId(Long auditId);
    Optional<AuditStepResult> findByAuditRequestIdAndProcessStepId(
            Long requestId, Long processStepId);
}
