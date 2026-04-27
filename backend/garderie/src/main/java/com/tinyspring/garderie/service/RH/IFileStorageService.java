package com.tinyspring.garderie.service.RH;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface IFileStorageService {

    String saveFile(MultipartFile file) throws IOException;

    void deleteFile(String fileName) throws IOException;
}
