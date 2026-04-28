package com.tinyspring.garderie.service.boutique;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String saveFile(MultipartFile file);
    void deleteFile(String imageUrl);
}