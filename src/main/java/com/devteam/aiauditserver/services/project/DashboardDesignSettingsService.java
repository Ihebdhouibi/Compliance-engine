package com.devteam.aiauditserver.services.project;

import com.devteam.aiauditserver.models.project.DashboardDesignSettings;
import com.devteam.aiauditserver.repositories.File.FilesStorageService;
import com.devteam.aiauditserver.repositories.project.DashboardDesignSettingsRepository;
import com.devteam.aiauditserver.requests.project.UpdateDashboardDesignRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DashboardDesignSettingsService {

    @Autowired
    private DashboardDesignSettingsRepository repository;

    @Autowired
    private FilesStorageService filesStorageService;

    public DashboardDesignSettings initOrGetSettings() {
        return repository.findTopByOrderByIdAsc()
                .orElseGet(() -> repository.save(new DashboardDesignSettings()));
    }

    public DashboardDesignSettings removeLogo() {
        DashboardDesignSettings settings = initOrGetSettings();
        settings.setLogo(null);
        return repository.save(settings);
    }

    public DashboardDesignSettings removeFavIcon() {
        DashboardDesignSettings settings = initOrGetSettings();
        settings.setFavIcon(null);
        return repository.save(settings);
    }

    public DashboardDesignSettings updateSettings(
            UpdateDashboardDesignRequest request,
            MultipartFile favIconFile,
            MultipartFile logoFile
    ) {
        DashboardDesignSettings settings = initOrGetSettings();

        if (favIconFile != null && !favIconFile.isEmpty()) {
            settings.setFavIcon(
                    filesStorageService.save_file(favIconFile, "dashboard/favicon")
            );
        }

        if (logoFile != null && !logoFile.isEmpty()) {
            settings.setLogo(
                    filesStorageService.save_file(logoFile, "dashboard/logo")
            );
        }

        if (request.getProjectTitle() != null)
            settings.setProjectTitle(request.getProjectTitle());

        if (request.getSideNavbarColor() != null)
            settings.setSideNavbarColor(request.getSideNavbarColor());

        if (request.getTopNavbarColor() != null)
            settings.setTopNavbarColor(request.getTopNavbarColor());

        if (request.getBackgroundColor() != null)
            settings.setBackgroundColor(request.getBackgroundColor());

        if (request.getAccentColor() != null)
            settings.setAccentColor(request.getAccentColor());

        if (request.getAddButtonColor() != null)
            settings.setAddButtonColor(request.getAddButtonColor());

        if (request.getUpdateButtonColor() != null)
            settings.setUpdateButtonColor(request.getUpdateButtonColor());

        if (request.getDeleteButtonColor() != null)
            settings.setDeleteButtonColor(request.getDeleteButtonColor());

        return repository.save(settings);
    }
}
