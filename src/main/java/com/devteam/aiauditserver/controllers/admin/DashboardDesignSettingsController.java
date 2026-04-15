package com.devteam.aiauditserver.controllers.admin;

import com.devteam.aiauditserver.Tools.util.BaseController;
import com.devteam.aiauditserver.models.project.DashboardDesignSettings;
import com.devteam.aiauditserver.requests.project.UpdateDashboardDesignRequest;
import com.devteam.aiauditserver.services.project.DashboardDesignSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@CrossOrigin
@RequestMapping("/api/v1/design-settings")
public class DashboardDesignSettingsController extends BaseController {
    @Autowired
    private DashboardDesignSettingsService service;

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardDesignSettings> updateSettings(
            @RequestPart(value = "data", required = false) UpdateDashboardDesignRequest request,
            @RequestPart(value = "favIcon", required = false) MultipartFile favIconFile,
            @RequestPart(value = "logo",    required = false) MultipartFile logoFile
    ) {
        if (request == null) request = new UpdateDashboardDesignRequest();
        DashboardDesignSettings updated = service.updateSettings(request, favIconFile, logoFile);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @PutMapping("remove-logo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardDesignSettings> removeLogoFromSettings() {
        DashboardDesignSettings updated = service.removeLogo();
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @PutMapping("remove-fav-icon")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardDesignSettings> removeFavIconFromSettings() {
        DashboardDesignSettings updated = service.removeFavIcon();
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }


}
