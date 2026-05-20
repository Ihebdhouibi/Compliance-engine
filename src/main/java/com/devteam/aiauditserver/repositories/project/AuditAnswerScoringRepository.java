package com.devteam.aiauditserver.repositories.project;

import com.devteam.aiauditserver.models.project.AuditRequest.AuditAnswerScoring;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditAnswerScoringRepository extends JpaRepository<AuditAnswerScoring, Long> {
    Optional<AuditAnswerScoring> findByAnswerId(Long answerId);

    @org.springframework.data.jpa.repository.Query(
        "SELECT s FROM AuditAnswerScoring s WHERE s.answer.auditRequest.id = :requestId")
    List<AuditAnswerScoring> findAllByAuditRequestId(
        @org.springframework.data.repository.query.Param("requestId") Long requestId);
}
