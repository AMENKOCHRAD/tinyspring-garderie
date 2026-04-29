package com.tinyspring.garderie.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface OrdonnanceStorageService {

    String storePdf(MultipartFile file);

    Resource loadAsResource(String storedFilename);
}

