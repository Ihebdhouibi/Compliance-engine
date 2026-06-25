package com.devteam.aiauditserver.controllers;

import com.devteam.aiauditserver.models.File.MediaModel;
import com.devteam.aiauditserver.repositories.File.MediaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    @Autowired
    private MediaRepository mediaRepository;

    @GetMapping("/{id}/preview")
    public ResponseEntity<String> getPreviewUrl(@PathVariable Long id) {
        MediaModel media = mediaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Media not found with id: " + id));
        String preview = media.getPreviewUrl();
        if (preview == null || preview.isEmpty()) {
            preview = media.getUrl(); // fallback
        }
        return ResponseEntity.ok(preview);
    }
}
