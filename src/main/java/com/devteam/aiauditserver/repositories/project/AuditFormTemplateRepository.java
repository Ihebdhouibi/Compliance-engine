package com.devteam.aiauditserver.repositories.project;

import com.devteam.aiauditserver.enums.Project.AuditLevel;
import com.devteam.aiauditserver.enums.Project.AuditType;
import com.devteam.aiauditserver.models.project.AuditForm.AuditFormTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditFormTemplateRepository extends JpaRepository<AuditFormTemplate, Long> {

    /**
     * Latest ACTIVE template for the given audit type. Defined as a query so
     * legacy callers that don't care about the level always get the most
     * recently active record (regardless of LEVEL_1 vs LEVEL_2 split).
     */
    default Optional<AuditFormTemplate> findByAuditType(AuditType auditType) {
        List<AuditFormTemplate> list = findActiveByAuditTypeOrdered(
                auditType, PageRequest.of(0, 1));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Query("SELECT t FROM AuditFormTemplate t " +
           "WHERE t.auditType = :auditType AND t.active = true " +
           "ORDER BY (CASE WHEN t.level = com.devteam.aiauditserver.enums.Project.AuditLevel.LEVEL_2 THEN 0 ELSE 1 END), " +
           "t.templateVersion DESC")
    List<AuditFormTemplate> findActiveByAuditTypeOrdered(
            @Param("auditType") AuditType auditType,
            org.springframework.data.domain.Pageable pageable);

    Optional<AuditFormTemplate> findByAuditTypeAndLevel(AuditType auditType, AuditLevel level);

    /**
     * Picks the newest ACTIVE template for the given type + level. Used by
     * the two-level flow so stale/legacy duplicates (e.g. older templates
     * seeded by {@code DefaultTemplateLoaderService} without a version)
     * are ignored.
     */
    Optional<AuditFormTemplate> findFirstByAuditTypeAndLevelAndActiveTrueOrderByTemplateVersionDescIdDesc(
            AuditType auditType, AuditLevel level);

    List<AuditFormTemplate> findAllByAuditTypeAndLevel(AuditType auditType, AuditLevel level);

    Optional<AuditFormTemplate> findFirstByLevelAndActiveTrueOrderByTemplateVersionDesc(AuditLevel level);

    List<AuditFormTemplate> findByActiveTrue();

    List<AuditFormTemplate> findByLevel(AuditLevel level);

    default boolean existsByAuditType(AuditType auditType) {
        return findByAuditType(auditType).isPresent();
    }

    boolean existsByAuditTypeAndLevel(AuditType auditType, AuditLevel level);
}
