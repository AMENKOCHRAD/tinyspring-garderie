package com.tinyspring.garderie.service.boutique;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${file.upload-dir:uploads/boutique}")
    private String uploadDir;

    @Override
    public String saveFile(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(
                        originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID() + extension;
            Files.copy(file.getInputStream(),
                    uploadPath.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING);
            return "/images/boutique/" + filename;
        } catch (IOException e) {
            throw new RuntimeException(
                    "Erreur lors de la sauvegarde du fichier : " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return;
        try {
            String filename = imageUrl.replace("/images/boutique/", "");
            Files.deleteIfExists(Paths.get(uploadDir).resolve(filename));
        } catch (IOException e) {
            // Ignorer si le fichier n'existe pas
        }
    }
}