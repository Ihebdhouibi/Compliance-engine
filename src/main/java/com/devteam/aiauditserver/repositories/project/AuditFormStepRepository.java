package com.devteam.aiauditserver.repositories.project;


import com.devteam.aiauditserver.models.project.AuditForm.AuditFormStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditFormStepRepository extends JpaRepository<AuditFormStep, Long> {
    List<AuditFormStep> findByTemplateIdOrderByStepOrderAsc(Long templateId);
}
