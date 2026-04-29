package com.tinyspring.garderie.service;

import com.tinyspring.garderie.dto.MlPredictionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MlPredictionServiceTest {

    private MlPredictionService service;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        service = new MlPredictionService();
        RestTemplate restTemplate = new RestTemplate();
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void predictCategoryReturnsMlResponse() {
        server.expect(requestTo("http://localhost:8000/predict-category"))
                .andRespond(withSuccess("""
                        {"predictedCategory":"TRANSPORT","classificationConfidence":0.91}
                        """, MediaType.APPLICATION_JSON));

        MlPredictionResponse response = service.predictCategory("Bus", "Retard du bus");

        assertEquals("TRANSPORT", response.getPredictedCategory());
        assertEquals(0.91, response.getClassificationConfidence());
        server.verify();
    }

    @Test
    void predictPriorityReturnsMlResponse() {
        server.expect(requestTo("http://localhost:8000/predict-priority"))
                .andRespond(withSuccess("""
                        {"predictedPriority":"HIGH","priorityConfidence":0.88}
                        """, MediaType.APPLICATION_JSON));

        MlPredictionResponse response = service.predictPriority("Securite", "Incident important");

        assertEquals("HIGH", response.getPredictedPriority());
        assertEquals(0.88, response.getPriorityConfidence());
        server.verify();
    }

    @Test
    void predictDecisionReturnsMlResponse() {
        server.expect(requestTo("http://localhost:8000/predict-decision"))
                .andRespond(withSuccess("""
                        {"decisionRecommendation":"PARENT_FOLLOWUP","decisionConfidence":0.77}
                        """, MediaType.APPLICATION_JSON));

        MlPredictionResponse response = service.predictDecision(
                "Suivi",
                "Besoin de precision",
                "ADMINISTRATIF",
                "MEDIUM"
        );

        assertEquals("PARENT_FOLLOWUP", response.getDecisionRecommendation());
        assertEquals(0.77, response.getDecisionConfidence());
        server.verify();
    }

    @Test
    void predictCategoryReturnsNullWhenMlServiceFails() {
        server.expect(requestTo("http://localhost:8000/predict-category"))
                .andRespond(withServerError());

        assertNull(service.predictCategory("titre", "description"));
        server.verify();
    }
}
