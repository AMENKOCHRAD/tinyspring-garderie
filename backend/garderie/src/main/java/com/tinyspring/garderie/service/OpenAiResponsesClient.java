package com.tinyspring.garderie.service;

import java.util.Map;

public interface OpenAiResponsesClient {

    boolean isEnabled();

    <T> T createJsonSchemaResponse(String system, String user, Map<String, Object> jsonSchema, Class<T> dtoClass);

    Map<String, Object> createJsonSchemaResponse(String system, String user, Map<String, Object> jsonSchema);
}

