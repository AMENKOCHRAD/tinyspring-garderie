package com.tinyspring.garderie.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiResponsesClientImpl implements OpenAiResponsesClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.openai.api-key:}")
    private String apiKey;

    @Value("${app.openai.model:gpt-4o-mini}")
    private String model;

    @Value("${app.openai.reasoning-effort:none}")
    private String reasoningEffort;

    @Value("${app.openai.timeout-seconds:20}")
    private long timeoutSeconds;

    public OpenAiResponsesClientImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public <T> T createJsonSchemaResponse(String system, String user, Map<String, Object> jsonSchema, Class<T> dtoClass) {
        if (!isEnabled()) {
            throw new RuntimeException("OpenAI non configure (OPENAI_API_KEY manquant).");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("input", List.of(
                    Map.of(
                            "role", "system",
                            "content", List.of(Map.of("type", "input_text", "text", system))
                    ),
                    Map.of(
                            "role", "user",
                            "content", List.of(Map.of("type", "input_text", "text", user))
                    )
            ));
            if (reasoningEffort != null && !reasoningEffort.isBlank() && !"none".equalsIgnoreCase(reasoningEffort.trim())) {
                body.put("reasoning", Map.of("effort", reasoningEffort.trim()));
            }
            body.put("tool_choice", "none");
            body.put("tools", List.of());
            body.put("text", Map.of(
                    "format", Map.of(
                            "type", "json_schema",
                            "name", "observation_ai",
                            "schema", jsonSchema,
                            "strict", true
                    )
            ));

            byte[] payload = objectMapper.writeValueAsBytes(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/responses"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();
            byte[] bytes = response.body() != null ? response.body() : new byte[0];
            String raw = new String(bytes, StandardCharsets.UTF_8);

            if (status < 200 || status >= 300) {
                String msg = "Erreur OpenAI (HTTP " + status + ").";
                try {
                    JsonNode err = objectMapper.readTree(raw);
                    String detail = err.path("error").path("message").asText(null);
                    if (detail != null && !detail.isBlank()) {
                        msg = msg + " " + detail.trim();
                    }
                } catch (Exception ignore) {
                    // keep generic
                }
                throw new RuntimeException(msg);
            }

            JsonNode root = objectMapper.readTree(raw);
            String outputText = root.path("output_text").asText(null);
            if (outputText == null || outputText.isBlank()) {
                // fallback: find first output_text in output array
                JsonNode output = root.path("output");
                if (output.isArray()) {
                    for (JsonNode item : output) {
                        JsonNode content = item.path("content");
                        if (content.isArray()) {
                            for (JsonNode part : content) {
                                if ("output_text".equals(part.path("type").asText())) {
                                    outputText = part.path("text").asText(null);
                                    break;
                                }
                            }
                        }
                        if (outputText != null && !outputText.isBlank()) break;
                    }
                }
            }

            if (outputText == null || outputText.isBlank()) {
                throw new RuntimeException("OpenAI: reponse vide.");
            }

            // Structured outputs returns JSON as text; parse into dto.
            return objectMapper.readValue(outputText, dtoClass);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Erreur OpenAI: " + ex.getMessage());
        }
    }

    public Map<String, Object> createJsonSchemaResponse(String system, String user, Map<String, Object> jsonSchema) {
        @SuppressWarnings("unchecked")
        Map<String, Object> out = (Map<String, Object>) createJsonSchemaResponse(system, user, jsonSchema, Object.class);
        return out != null ? out : Map.of();
    }
}
