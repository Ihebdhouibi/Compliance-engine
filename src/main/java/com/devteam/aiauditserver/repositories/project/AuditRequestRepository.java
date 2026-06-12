package com.devteam.aiauditserver.repositories.project;


import com.devteam.aiauditserver.enums.Project.AuditStatus;
import com.devteam.aiauditserver.models.project.AuditRequest.AuditRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditRequestRepository extends JpaRepository<AuditRequest, Long> {

    // All requests submitted by a specific user
    Page<AuditRequest> findBySubmittedByIdOrderBySubmittedAtDesc(Long userId, Pageable pageable);

    // All requests assigned to a specific auditor/admin
    Page<AuditRequest> findByAssignedToIdOrderBySubmittedAtDesc(Long userId, Pageable pageable);

    // Admin: all requests with optional status filter
    Page<AuditRequest> findByStatusOrderBySubmittedAtDesc(AuditStatus status, Pageable pageable);

    // Admin: all requests regardless of status
    Page<AuditRequest> findAllByOrderBySubmittedAtDesc(Pageable pageable);

    // Search by company name or submitter email
    @Query("SELECT r FROM AuditRequest r WHERE " +
            "LOWER(r.submittedBy.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.submittedBy.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(r.submittedBy.companyInfo.companyName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<AuditRequest> searchRequests(@Param("search") String search, Pageable pageable);

    // Notifications: all submitted requests (no pagination)
    List<AuditRequest> findByStatusOrderBySubmittedAtDesc(AuditStatus status);

    // Count by status for dashboard stats
    long countByStatus(AuditStatus status);

    // Count assigned to a specific user
    long countByAssignedToId(Long userId);
}
