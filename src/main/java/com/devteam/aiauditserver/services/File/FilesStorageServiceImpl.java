package com.devteam.aiauditserver.services.File;

import com.devteam.aiauditserver.models.File.MediaModel;
import com.devteam.aiauditserver.repositories.File.FilesStorageService;
import com.devteam.aiauditserver.repositories.File.MediaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class FilesStorageServiceImpl implements FilesStorageService {
    private static final Logger logger = LoggerFactory.getLogger(FilesStorageServiceImpl.class);

    @Autowired
    private MediaRepository repository;

    private static final List<String> FILES_ACCEPTED_TYPE = Arrays.asList(
            "jpeg", "jpg", "png", "svg", "webp", "pdf", "mp4",
            "doc", "docx", "xls", "xlsx", "ppt", "pptx"
    );

    private static final Map<String, String> MIME_TO_EXTENSION = new HashMap<>();
    static {
        MIME_TO_EXTENSION.put("application/msword", "doc");
        MIME_TO_EXTENSION.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");
        MIME_TO_EXTENSION.put("application/vnd.ms-excel", "xls");
        MIME_TO_EXTENSION.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");
        MIME_TO_EXTENSION.put("application/vnd.ms-powerpoint", "ppt");
        MIME_TO_EXTENSION.put("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx");
        MIME_TO_EXTENSION.put("application/pdf", "pdf");
    }

    // Convert a .docx/.doc to PDF (for preview) using LibreOffice headless.
    private Path convertDocxToPdf(Path docxPath, String directory) throws IOException, InterruptedException {
        String os = System.getProperty("os.name").toLowerCase();
        String command = os.contains("win")
                ? "C:\\Program Files\\LibreOffice\\program\\soffice.exe"
                : "soffice";

        if (!Files.exists(docxPath)) {
            throw new IOException("Input file does not exist: " + docxPath);
        }

        Path outputDir = Paths.get("WebContent/" + directory + "/preview");
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        String fileName = docxPath.getFileName().toString();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        String pdfName = baseName + ".pdf";
        Path pdfPath = outputDir.resolve(pdfName);

        Files.deleteIfExists(pdfPath);

        ProcessBuilder pb = new ProcessBuilder(
                command,
                "--headless",
                "--convert-to", "pdf",
                "--outdir", outputDir.toString(),
                docxPath.toString()
        );
        pb.redirectErrorStream(true);

        logger.info("Converting {} to PDF using LibreOffice", docxPath);
        Process process = pb.start();
        boolean finished = process.waitFor(120, TimeUnit.SECONDS); // timeout 2 min

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Conversion timed out after 120 seconds");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                throw new RuntimeException("LibreOffice conversion failed with exit code " + exitCode + ": " + output);
            }
        }

        if (!Files.exists(pdfPath)) {
            throw new RuntimeException("PDF was not generated at: " + pdfPath);
        }

        logger.info("PDF generated successfully: {}", pdfPath);
        return pdfPath;
    }

    @Override
    public MediaModel save_file(MultipartFile file, String directory) {
        try {
            // 1. Resolve the extension from the ORIGINAL FILENAME. Deriving it
            //    from the MIME subtype breaks office formats — a .docx arrives
            //    as "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            //    whose subtype is a long vnd.* string that never matches a clean
            //    extension. Only when the filename has no usable extension, fall
            //    back to a known MIME->extension mapping, then to the raw subtype.
            String original = file.getOriginalFilename();
            String extension = "";
            if (original != null && original.contains(".")) {
                extension = original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            }
            if (extension.isEmpty()) {
                String contentType = file.getContentType();
                if (contentType != null && MIME_TO_EXTENSION.containsKey(contentType)) {
                    extension = MIME_TO_EXTENSION.get(contentType);
                } else if (contentType != null && contentType.contains("/")) {
                    extension = contentType.substring(contentType.indexOf("/") + 1).toLowerCase(Locale.ROOT);
                }
            }
            if (extension.contains("+xml") && extension.contains("svg")) extension = "svg";

            if (!FILES_ACCEPTED_TYPE.contains(extension)) {
                throw new RuntimeException("Format not supported: " + extension);
            }

            // 2. Enregistrer le fichier physique
            Path root = Paths.get("WebContent/" + directory);
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }

            String fileName = UUID.randomUUID().toString() + "_" + new Date().getTime() + "." + extension;
            Path filepath = root.resolve(fileName);
            Files.copy(file.getInputStream(), filepath, StandardCopyOption.REPLACE_EXISTING);

            String browserUrl = "/WebContent/" + directory + "/" + fileName;

            // 3. Créer le MediaModel original
            MediaModel mediaResult = new MediaModel(
                    fileName,
                    browserUrl,
                    file.getContentType(),
                    file.getSize()
            );
            mediaResult = repository.save(mediaResult);

            // 4. Si c'est un .docx, générer le PDF de prévisualisation
            if ("docx".equals(extension) || "doc".equals(extension)) {
                logger.info("Démarrage de la conversion pour : {}", fileName);
                try {
                    Path pdfPath = convertDocxToPdf(filepath, directory);
                    String pdfUrl = "/WebContent/" + directory + "/preview/" + pdfPath.getFileName().toString();

                    MediaModel pdfMedia = new MediaModel(
                            pdfPath.getFileName().toString(),
                            pdfUrl,
                            "application/pdf",
                            Files.size(pdfPath)
                    );
                    pdfMedia = repository.save(pdfMedia);

                    mediaResult.setPreviewUrl(pdfMedia.getUrl());
                    mediaResult = repository.save(mediaResult);

                    logger.info("Preview PDF généré pour {}", fileName);
                } catch (Exception e) {
                    logger.error("Échec de la conversion pour {}", fileName, e);
                    // On ne bloque pas l'upload
                }
            }

            return mediaResult;

        } catch (Exception e) {
            logger.error("Erreur interne lors de la sauvegarde du fichier : ", e);
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
