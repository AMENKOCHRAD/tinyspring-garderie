package com.tinyspring.garderie.service.RH;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests OllamaServiceImpl")
class OllamaServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OllamaServiceImpl service;

    @BeforeEach
    void setUp() {
        // ✅ Injection du RestTemplate interne via ReflectionTestUtils
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(service, "apiUrl", "http://localhost:11434/api/generate");
        ReflectionTestUtils.setField(service, "model", "tinyspring-rh-v2");
    }

    // ===== generer — succès =====

    @Test
    @DisplayName("generer — doit retourner la réponse d'Ollama")
    void generer_doitRetournerReponse() {
        Map<String, Object> body = Map.of("response", "Voici le rapport RH généré.");

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> responseEntity = (ResponseEntity<Map>) mock(ResponseEntity.class);
        when(responseEntity.getBody()).thenReturn(body);

        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        String result = service.generer("Génère un rapport RH");

        assertThat(result).isEqualTo("Voici le rapport RH généré.");
    }

    @Test
    @DisplayName("generer — doit nettoyer le préfixe ### Response:")
    void generer_doitNettoyerPrefixe() {
        Map<String, Object> body = Map.of("response", "### Response: Rapport nettoyé");

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> responseEntity = (ResponseEntity<Map>) mock(ResponseEntity.class);
        when(responseEntity.getBody()).thenReturn(body);

        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        String result = service.generer("Question");

        assertThat(result).isEqualTo("Rapport nettoyé");
        assertThat(result).doesNotContain("### Response:");
    }

    @Test
    @DisplayName("generer — doit retourner message erreur si body null")
    void generer_doitRetournerErreurSiBodyNull() {
        @SuppressWarnings("unchecked")
        ResponseEntity<Map> responseEntity = (ResponseEntity<Map>) mock(ResponseEntity.class);
        when(responseEntity.getBody()).thenReturn(null);

        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        String result = service.generer("Question");

        assertThat(result).contains("Erreur");
    }

    @Test
    @DisplayName("generer — doit retourner message erreur si response null dans body")
    void generer_doitRetournerErreurSiResponseNull() {
        Map<String, Object> body = Map.of(); // pas de clé "response"

        @SuppressWarnings("unchecked")
        ResponseEntity<Map> responseEntity = (ResponseEntity<Map>) mock(ResponseEntity.class);
        when(responseEntity.getBody()).thenReturn(body);

        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(responseEntity);

        String result = service.generer("Question");

        assertThat(result).contains("Erreur");
    }

    @Test
    @DisplayName("generer — doit gérer exception réseau et retourner message d'erreur")
    void generer_doitGererExceptionReseau() {
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("Connection refused"));

        String result = service.generer("Question");

        assertThat(result).contains("Erreur connexion Ollama");
    }
}
