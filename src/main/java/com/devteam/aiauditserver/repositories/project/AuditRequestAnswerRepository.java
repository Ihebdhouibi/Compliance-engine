package com.devteam.aiauditserver.repositories.project;

import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequestAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditRequestAnswerRepository
        extends JpaRepository<AuditRequestAnswer, Long> {

    Optional<AuditRequestAnswer> findByAuditRequestIdAndFieldId(
            Long auditRequestId, Long fieldId);

    List<AuditRequestAnswer> findByAuditRequestId(Long auditId);
}
