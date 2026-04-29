package com.tinyspring.garderie.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrdonnanceStorageServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void storePdf_sauvegardeFichierQuandSignaturePdfOk() throws Exception {
        OrdonnanceStorageServiceImpl service = new OrdonnanceStorageServiceImpl(tempDir.toString());
        byte[] pdf = "%PDF-1.7\n%....".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "ordonnance.pdf", "application/pdf", pdf);

        String stored = service.storePdf(file);

        assertThat(stored).endsWith(".pdf");
        assertThat(Files.exists(tempDir.resolve(stored))).isTrue();
    }

    @Test
    void storePdf_refuseSiExtensionPasPdf() {
        OrdonnanceStorageServiceImpl service = new OrdonnanceStorageServiceImpl(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "ordonnance.txt", "text/plain", "%PDF-".getBytes());

        assertThatThrownBy(() -> service.storePdf(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("PDF");
    }

    @Test
    void storePdf_refuseSiHeaderPasPdf() {
        OrdonnanceStorageServiceImpl service = new OrdonnanceStorageServiceImpl(tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "ordonnance.pdf", "application/pdf", "NOTPDF".getBytes());

        assertThatThrownBy(() -> service.storePdf(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("invalide");
    }

    @Test
    void loadAsResource_refuseCheminSuspect() {
        OrdonnanceStorageServiceImpl service = new OrdonnanceStorageServiceImpl(tempDir.toString());
        assertThatThrownBy(() -> service.loadAsResource("../hack.pdf"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Chemin");
    }
}

