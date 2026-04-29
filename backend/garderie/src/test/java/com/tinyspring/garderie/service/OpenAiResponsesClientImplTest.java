package com.tinyspring.garderie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiResponsesClientImplTest {

    @Test
    void isEnabled_falseSiApiKeyVide() {
        OpenAiResponsesClientImpl client = new OpenAiResponsesClientImpl(new ObjectMapper());
        ReflectionTestUtils.setField(client, "apiKey", "");
        assertThat(client.isEnabled()).isFalse();
    }

    @Test
    void createJsonSchemaResponse_throwSiNonConfigure() {
        OpenAiResponsesClientImpl client = new OpenAiResponsesClientImpl(new ObjectMapper());
        ReflectionTestUtils.setField(client, "apiKey", " ");

        assertThatThrownBy(() -> client.createJsonSchemaResponse("sys", "user", Map.of("type", "object")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("OpenAI non configure");
    }
}

