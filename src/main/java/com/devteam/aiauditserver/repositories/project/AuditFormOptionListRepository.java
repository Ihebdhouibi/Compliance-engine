package com.devteam.aiauditserver.repositories.project;

import com.devteam.aiauditserver.models.project.AuditForm.AuditFormOptionList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuditFormOptionListRepository extends JpaRepository<AuditFormOptionList, Long> {
    Optional<AuditFormOptionList> findByListKey(String listKey);
    boolean existsByListKey(String listKey);
}
