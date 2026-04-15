package com.devteam.aiauditserver.repositories.File;

import com.devteam.aiauditserver.models.File.MediaModel;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

public interface FilesStorageService {
    // Saves a single file and returns the MediaModel
    MediaModel save_file(MultipartFile file, String directory);

    // Deletes a file from the disk and the record from the database
    void delete_file_by_path(String path, Long imageid);

    // Saves multiple files at once
    Set<MediaModel> save_all(List<MultipartFile> files, String directory, String name);
}
