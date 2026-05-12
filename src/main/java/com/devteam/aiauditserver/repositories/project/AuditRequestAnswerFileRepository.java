package com.devteam.aiauditserver.repositories.project;

import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequestAnswerFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditRequestAnswerFileRepository extends JpaRepository<AuditRequestAnswerFile, Long> {
}
