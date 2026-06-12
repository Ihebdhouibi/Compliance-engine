package com.devteam.aiauditserver.repositories.project;

import com.devteam.aiauditserver.models.project.AuditRequest.AuditProcessStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditProcessStepRepository extends JpaRepository<AuditProcessStep, Long> {
    List<AuditProcessStep> findByTemplateIdOrderByStepOrderAsc(Long templateId);
    long countByTemplateId(Long templateId);
}
