package com.devteam.aiauditserver.repositories.project;

import com.devteam.aiauditserver.models.project.AuditRequest.EvidenceOcrResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvidenceOcrResultRepository extends JpaRepository<EvidenceOcrResult, Long> {
    List<EvidenceOcrResult> findByAuditRequestIdOrderByCreatedAtAsc(Long auditRequestId);
    Optional<EvidenceOcrResult> findByAuditRequestIdAndMediaId(Long auditRequestId, Long mediaId);
    Optional<EvidenceOcrResult> findByJobId(String jobId);
}
