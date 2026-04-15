package com.devteam.aiauditserver.controllers.publicControllers;

import com.devteam.aiauditserver.models.project.DashboardDesignSettings;
import com.devteam.aiauditserver.services.project.DashboardDesignSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequestMapping("/api/v1/public")
public class PublicController {

    @Autowired
    private DashboardDesignSettingsService service;

    @GetMapping("dashboard-design-settings")
    public ResponseEntity<DashboardDesignSettings> getSettings() {
        return ResponseEntity.ok(service.initOrGetSettings());
    }


}
