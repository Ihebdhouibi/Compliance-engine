package com.devteam.aiauditserver.services.File;

import com.devteam.aiauditserver.models.File.MediaModel;
import com.devteam.aiauditserver.repositories.File.FilesStorageService;
import com.devteam.aiauditserver.repositories.File.MediaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;


@Service
public class FilesStorageServiceImpl implements FilesStorageService {
    private static final Logger logger = LoggerFactory.getLogger(FilesStorageServiceImpl.class);

    @Autowired
    private MediaRepository repository;

    private static final List<String> FILES_ACCEPTED_TYPE = Arrays.asList(
            "jpeg", "jpg", "png", "svg", "webp", "pdf", "mp4",
            "docx", "doc", "xlsx", "xls"
    );

    @Override
    public MediaModel save_file(MultipartFile file, String directory) {
        try {
            // 1. Resolve the extension from the ORIGINAL FILENAME. Deriving it
            //    from the MIME subtype breaks office formats — a .docx arrives
            //    as "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            //    whose subtype is a long vnd.* string that never matches a clean
            //    extension. Fall back to the MIME subtype only when the filename
            //    has no extension.
            String original = file.getOriginalFilename();
            String extension = "";
            if (original != null && original.contains(".")) {
                extension = original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            }
            if (extension.isEmpty()) {
                String contentType = file.getContentType();
                if (contentType != null && contentType.contains("/")) {
                    extension = contentType.substring(contentType.indexOf("/") + 1).toLowerCase(Locale.ROOT);
                }
            }
            if (extension.contains("+xml") && extension.contains("svg")) extension = "svg";

            if (!FILES_ACCEPTED_TYPE.contains(extension)) {
                throw new RuntimeException("Format not supported: " + extension);
            }

            // 2. Define the root directory
            Path root = Paths.get("WebContent/" + directory);
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }

            // 3. FIX: Create a unique FILENAME, not a path
            // Use UUID or just the timestamp. Don't prepend the 'directory' string here.
            String fileName = UUID.randomUUID().toString() + "_" + new Date().getTime() + "." + extension;

            // This resolves to WebContent/GymRequests/IDs/filename.pdf
            Path filepath = root.resolve(fileName);

            Files.copy(file.getInputStream(), filepath, StandardCopyOption.REPLACE_EXISTING);

            // 4. Set the URL for the browser
            String browserUrl = "/WebContent/" + directory + "/" + fileName;

            MediaModel mediaResult = new MediaModel(
                    fileName,
                    browserUrl,
                    file.getContentType(),
                    file.getSize()
            );

            return this.repository.save(mediaResult);

        } catch (Exception e) {
            // Log the actual cause so you can see if it's an Access Denied or File Not Found issue
            logger.error("Internal error saving file: ", e);
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }

    @Override
    public void delete_file_by_path(String path, Long imageid) {
        try {
            String physicalPath = path.startsWith("/") ? path.substring(1) : path;
            Path root_local = Paths.get(physicalPath);

            if (this.repository.existsById(imageid)) {
                this.repository.deleteById(imageid);
            }
            Files.deleteIfExists(root_local);
        } catch (IOException e) {
            throw new RuntimeException("Error deleting file: " + e.getMessage());
        }
    }

    @Override
    public Set<MediaModel> save_all(List<MultipartFile> files, String directory, String name) {
        Set<MediaModel> images = new HashSet<>();
        for (int i = 0; i < files.size(); i++) {
            MediaModel uploaded = this.save_file(files.get(i), directory);
            uploaded.setRange(i);
            images.add(this.repository.save(uploaded));
        }
        return images;
    }


}
