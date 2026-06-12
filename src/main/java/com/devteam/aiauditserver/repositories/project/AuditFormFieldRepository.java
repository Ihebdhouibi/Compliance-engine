package com.devteam.aiauditserver.repositories.project;


import com.devteam.aiauditserver.models.project.AuditForm.AuditFormField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditFormFieldRepository extends JpaRepository<AuditFormField, Long> {
    List<AuditFormField> findByStepIdOrderByFieldOrderAsc(Long stepId);
}
