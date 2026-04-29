package com.tinyspring.garderie.service.RH;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests FileStorageServiceImpl")
class FileStorageServiceImplTest {

    @TempDir
    Path tempDir;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private FileStorageServiceImpl service;

    @BeforeEach
    void setUp() {
        // ✅ Injection du dossier temporaire via ReflectionTestUtils
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
    }

    // ===== saveFile =====

    @Test
    @DisplayName("saveFile — doit sauvegarder le fichier et retourner le nom généré")
    void saveFile_doitSauvegarderEtRetournerNom() throws IOException {
        when(multipartFile.getOriginalFilename()).thenReturn("photo.jpg");
        when(multipartFile.getInputStream())
                .thenReturn(new ByteArrayInputStream("image content".getBytes()));

        String fileName = service.saveFile(multipartFile);

        assertThat(fileName).isNotNull();
        assertThat(fileName).endsWith(".jpg");
        // Le fichier doit exister dans le répertoire temporaire
        assertThat(Files.exists(tempDir.resolve(fileName))).isTrue();
    }

    @Test
    @DisplayName("saveFile — doit gérer extension png")
    void saveFile_doitGererExtensionPng() throws IOException {
        when(multipartFile.getOriginalFilename()).thenReturn("photo.png");
        when(multipartFile.getInputStream())
                .thenReturn(new ByteArrayInputStream("image content".getBytes()));

        String fileName = service.saveFile(multipartFile);

        assertThat(fileName).endsWith(".png");
    }

    @Test
    @DisplayName("saveFile — doit utiliser extension jpg si nom null")
    void saveFile_doitUtiliserJpgSiNomNull() throws IOException {
        when(multipartFile.getOriginalFilename()).thenReturn(null);
        when(multipartFile.getInputStream())
                .thenReturn(new ByteArrayInputStream("content".getBytes()));

        String fileName = service.saveFile(multipartFile);

        assertThat(fileName).endsWith(".jpg");
    }

    @Test
    @DisplayName("saveFile — doit utiliser jpg si pas d'extension")
    void saveFile_doitUtiliserJpgSiPasExtension() throws IOException {
        when(multipartFile.getOriginalFilename()).thenReturn("filenameWithoutExtension");
        when(multipartFile.getInputStream())
                .thenReturn(new ByteArrayInputStream("content".getBytes()));

        String fileName = service.saveFile(multipartFile);

        assertThat(fileName).endsWith(".jpg");
    }

    @Test
    @DisplayName("saveFile — doit créer le répertoire si inexistant")
    void saveFile_doitCreerRepertoire() throws IOException {
        // Sous-dossier qui n'existe pas encore
        Path subDir = tempDir.resolve("nouveau_dossier");
        ReflectionTestUtils.setField(service, "uploadDir", subDir.toString());

        when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");
        when(multipartFile.getInputStream())
                .thenReturn(new ByteArrayInputStream("content".getBytes()));

        String fileName = service.saveFile(multipartFile);

        assertThat(Files.exists(subDir)).isTrue();
        assertThat(fileName).endsWith(".jpg");
    }

    // ===== deleteFile =====

    @Test
    @DisplayName("deleteFile — doit supprimer le fichier existant")
    void deleteFile_doitSupprimerFichierExistant() throws IOException {
        // Créer un fichier temporaire
        Path fichier = tempDir.resolve("photo.jpg");
        Files.write(fichier, "image".getBytes());
        assertThat(Files.exists(fichier)).isTrue();

        service.deleteFile("photo.jpg");

        assertThat(Files.exists(fichier)).isFalse();
    }

    @Test
    @DisplayName("deleteFile — doit ignorer si fichier inexistant")
    void deleteFile_doitIgnorerSiFichierInexistant() {
        assertThatCode(() -> service.deleteFile("inexistant.jpg"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("deleteFile — doit ignorer si nom null")
    void deleteFile_doitIgnorerSiNomNull() {
        assertThatCode(() -> service.deleteFile(null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("deleteFile — doit ignorer si nom vide")
    void deleteFile_doitIgnorerSiNomVide() {
        assertThatCode(() -> service.deleteFile(""))
                .doesNotThrowAnyException();
    }
}
