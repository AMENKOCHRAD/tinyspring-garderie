package com.tinyspring.garderie.service.events;

import com.tinyspring.garderie.dto.Events.EventRecommendationContextRequest;
import com.tinyspring.garderie.dto.Events.EventRecommendationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventRecommendationServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private EventRecommendationServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "pythonAiUrl", "http://localhost:5006");
    }

    @Test
    void recommendEvents_shouldReturnRecommendations_whenPythonServiceReturnsBody() {
        EventRecommendationContextRequest request = EventRecommendationContextRequest.builder()
                .season("spring")
                .month("April")
                .ageGroup("3-5")
                .budgetLevel("LOW")
                .outdoorPreferred(true)
                .cityContext("Tunis")
                .build();

        EventRecommendationResponse recommendation = EventRecommendationResponse.builder()
                .title("Atelier jardinage")
                .description("Activité extérieure pour enfants")
                .build();

        when(restTemplate.postForEntity(
                eq("http://localhost:5006/recommend-events"),
                any(),
                eq(EventRecommendationResponse[].class)
        )).thenReturn(ResponseEntity.ok(new EventRecommendationResponse[]{recommendation}));

        List<EventRecommendationResponse> result = service.recommendEvents(request);

        assertEquals(1, result.size());
        assertEquals("Atelier jardinage", result.get(0).getTitle());

        verify(restTemplate).postForEntity(
                eq("http://localhost:5006/recommend-events"),
                any(),
                eq(EventRecommendationResponse[].class)
        );
    }

    @Test
    void recommendEvents_shouldSendCorrectPayloadToPythonService() {
        EventRecommendationContextRequest request = EventRecommendationContextRequest.builder()
                .season("winter")
                .month("January")
                .ageGroup("4-6")
                .budgetLevel("MEDIUM")
                .outdoorPreferred(null)
                .cityContext("Sousse")
                .build();

        when(restTemplate.postForEntity(
                anyString(),
                any(),
                eq(EventRecommendationResponse[].class)
        )).thenReturn(ResponseEntity.ok(new EventRecommendationResponse[]{}));

        service.recommendEvents(request);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);

        verify(restTemplate).postForEntity(
                eq("http://localhost:5006/recommend-events"),
                payloadCaptor.capture(),
                eq(EventRecommendationResponse[].class)
        );

        Object payload = payloadCaptor.getValue();

        assertEquals("winter", ReflectionTestUtils.getField(payload, "season"));
        assertEquals("January", ReflectionTestUtils.getField(payload, "month"));
        assertEquals("4-6", ReflectionTestUtils.getField(payload, "ageGroup"));
        assertEquals("MEDIUM", ReflectionTestUtils.getField(payload, "budgetLevel"));
        assertEquals(false, ReflectionTestUtils.getField(payload, "outdoorPreferred"));
        assertEquals("Sousse", ReflectionTestUtils.getField(payload, "cityContext"));
        assertEquals(8, ReflectionTestUtils.getField(payload, "topN"));
    }

    @Test
    void recommendEvents_shouldReturnEmptyList_whenPythonServiceReturnsNullBody() {
        EventRecommendationContextRequest request = new EventRecommendationContextRequest();

        when(restTemplate.postForEntity(
                anyString(),
                any(),
                eq(EventRecommendationResponse[].class)
        )).thenReturn(ResponseEntity.ok(null));

        List<EventRecommendationResponse> result = service.recommendEvents(request);

        assertTrue(result.isEmpty());
    }

    @Test
    void recommendEvents_shouldThrowIllegalStateException_whenPythonServiceFails() {
        EventRecommendationContextRequest request = new EventRecommendationContextRequest();

        when(restTemplate.postForEntity(
                anyString(),
                any(),
                eq(EventRecommendationResponse[].class)
        )).thenThrow(new RuntimeException("Python service down"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.recommendEvents(request)
        );

        assertEquals("Impossible d'obtenir les recommandations IA", exception.getMessage());
        assertNotNull(exception.getCause());
    }
}