package com.tinyspring.garderie.service.boutique;

public interface OllamaService {
    String analyserProduit(String nom, String description,
                           double prix, int stock,
                           int ventes, String categorie);
}