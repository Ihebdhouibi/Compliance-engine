package com.devteam.aiauditserver.repositories.project;

import com.devteam.aiauditserver.enums.Project.AuditType;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface AuditFormTemplateRepository extends JpaRepository<AuditFormTemplate, Long> {
    Optional<AuditFormTemplate> findByAuditType(AuditType auditType);
    List<AuditFormTemplate> findByActiveTrue();
    boolean existsByAuditType(AuditType auditType);
}
