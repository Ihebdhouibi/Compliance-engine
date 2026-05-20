package com.devteam.aiauditserver.repositories.project;

import com.devteam.aiauditserver.models.project.AuditRequest.RoutingProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoutingProfileRepository extends JpaRepository<RoutingProfile, Long> {
    Optional<RoutingProfile> findByAuditRequestId(Long auditRequestId);
}
