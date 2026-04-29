package com.tinyspring.garderie.service.boutique;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests FileStorageService")
class FileStorageServiceTest {

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private FileStorageServiceImpl fileStorageService;

    private Path uploadDir;

    @BeforeEach
    void setUp() {
        uploadDir = Path.of(
                "target",
                "test-uploads",
                "file-storage-service-test",
                UUID.randomUUID().toString()
        );
        ReflectionTestUtils.setField(fileStorageService, "uploadDir", uploadDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.notExists(uploadDir)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(uploadDir)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // Best effort cleanup for generated test files.
                        }
                    });
        }
    }

    @Test
    @DisplayName("saveFile() sauvegarde un fichier avec extension")
    void saveFile_shouldSaveFileWithExtension() throws IOException {
        when(multipartFile.getOriginalFilename()).thenReturn("photo.png");
        when(multipartFile.getInputStream())
                .thenReturn(new ByteArrayInputStream("image".getBytes(StandardCharsets.UTF_8)));

        String imageUrl = fileStorageService.saveFile(multipartFile);

        String filename = imageUrl.replace("/images/boutique/", "");
        Path savedFile = uploadDir.resolve(filename);
        assertThat(imageUrl).startsWith("/images/boutique/");
        assertThat(filename).endsWith(".png");
        assertThat(savedFile).exists().hasContent("image");
    }

    @Test
    @DisplayName("saveFile() sauvegarde un fichier sans extension")
    void saveFile_shouldSaveFileWithoutExtension() throws IOException {
        when(multipartFile.getOriginalFilename()).thenReturn("photo");
        when(multipartFile.getInputStream())
                .thenReturn(new ByteArrayInputStream("raw".getBytes(StandardCharsets.UTF_8)));

        String imageUrl = fileStorageService.saveFile(multipartFile);

        String filename = imageUrl.replace("/images/boutique/", "");
        assertThat(imageUrl).startsWith("/images/boutique/");
        assertThat(filename).doesNotContain(".");
        assertThat(uploadDir.resolve(filename)).exists().hasContent("raw");
    }

    @Test
    @DisplayName("saveFile() transforme une IOException en RuntimeException")
    void saveFile_shouldThrowRuntimeExceptionWhenCopyFails() throws IOException {
        when(multipartFile.getOriginalFilename()).thenReturn("broken.jpg");
        when(multipartFile.getInputStream()).thenThrow(new IOException("lecture impossible"));

        assertThatThrownBy(() -> fileStorageService.saveFile(multipartFile))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Erreur lors de la sauvegarde du fichier")
                .hasMessageContaining("lecture impossible");
    }

    @Test
    @DisplayName("deleteFile() ignore une URL null")
    void deleteFile_shouldIgnoreNullUrl() {
        assertThatCode(() -> fileStorageService.deleteFile(null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("deleteFile() ignore une URL vide")
    void deleteFile_shouldIgnoreEmptyUrl() {
        assertThatCode(() -> fileStorageService.deleteFile(""))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("deleteFile() supprime un fichier existant")
    void deleteFile_shouldDeleteExistingFile() throws IOException {
        Files.createDirectories(uploadDir);
        Path savedFile = uploadDir.resolve("image.png");
        Files.writeString(savedFile, "image", StandardCharsets.UTF_8);

        fileStorageService.deleteFile("/images/boutique/image.png");

        assertThat(savedFile).doesNotExist();
    }
}
