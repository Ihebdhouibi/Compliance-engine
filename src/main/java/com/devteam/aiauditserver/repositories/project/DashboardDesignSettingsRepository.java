package com.devteam.aiauditserver.repositories.project;

import com.devteam.aiauditserver.models.project.DashboardDesignSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DashboardDesignSettingsRepository extends JpaRepository<DashboardDesignSettings, Long> {
    Optional<DashboardDesignSettings> findTopByOrderByIdAsc();
}