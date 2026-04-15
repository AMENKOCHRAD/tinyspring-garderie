package com.tinyspring.garderie.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class OrdonnanceStorageService {

    private final Path ordonnancesDir;

    public OrdonnanceStorageService(@Value("${app.uploads.ordonnances-dir:uploads/ordonnances}") String ordonnancesDir) {
        this.ordonnancesDir = Paths.get(ordonnancesDir).toAbsolutePath().normalize();
    }

    public String storePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Fichier ordonnance manquant.");
        }

        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "ordonnance.pdf";
        String lower = original.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".pdf")) {
            throw new RuntimeException("L'ordonnance doit etre un fichier PDF (.pdf).");
        }

        if (!looksLikePdf(file)) {
            throw new RuntimeException("Fichier invalide: ce n'est pas un PDF.");
        }

        try {
            Files.createDirectories(ordonnancesDir);
        } catch (IOException exception) {
            throw new RuntimeException("Impossible de creer le dossier d'uploads.");
        }

        String safeName = UUID.randomUUID() + ".pdf";
        Path target = ordonnancesDir.resolve(safeName).normalize();
        if (!target.startsWith(ordonnancesDir)) {
            throw new RuntimeException("Chemin fichier invalide.");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new RuntimeException("Impossible d'enregistrer l'ordonnance.");
        }

        return safeName;
    }

    public Resource loadAsResource(String storedFilename) {
        if (storedFilename == null || storedFilename.isBlank()) {
            throw new RuntimeException("Ordonnance introuvable.");
        }

        Path file = ordonnancesDir.resolve(storedFilename).normalize();
        if (!file.startsWith(ordonnancesDir)) {
            throw new RuntimeException("Chemin fichier invalide.");
        }

        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("Ordonnance introuvable.");
            }
            return resource;
        } catch (MalformedURLException exception) {
            throw new RuntimeException("Ordonnance introuvable.");
        }
    }

    private boolean looksLikePdf(MultipartFile file) {
        // PDF signature: "%PDF-"
        try (InputStream in = file.getInputStream()) {
            byte[] header = new byte[5];
            int read = in.read(header);
            if (read < 5) {
                return false;
            }
            return header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46 && header[4] == 0x2D;
        } catch (IOException exception) {
            return false;
        }
    }
}

