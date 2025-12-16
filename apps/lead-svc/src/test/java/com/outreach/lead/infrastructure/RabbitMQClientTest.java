package com.outreach.lead.infrastructure;

import com.outreach.lead.domain.ProspectCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitMQClientTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private RabbitMQClient rabbitMQClient;

    @BeforeEach
    void setUp() {
        rabbitMQClient = new RabbitMQClient(
            rabbitTemplate,
            "test.request.queue",
            "test.response.queue"
        );
    }

    @Test
    void testGenerateMatchingCompanies_Success() {
        // Given
        ProspectCriteria criteria = new ProspectCriteria(
            null, null, "Tech", 100, 500, null, null, null, null, null,
            List.of("Java"), null, null, null, null, null, null,
            List.of("US"), null, null, List.of("AI"), null, null
        );

        // When
        rabbitMQClient.generateMatchingCompanies(criteria, 5, 123);

        // Then
        verify(rabbitTemplate).convertAndSend(eq("test.request.queue"), any(Map.class));
    }

    @Test
    void testGenerateMatchingCompanies_WithNullBatchId() {
        // Given
        ProspectCriteria criteria = new ProspectCriteria(
            null, null, "Tech", null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null
        );

        // When
        rabbitMQClient.generateMatchingCompanies(criteria, 10, null);

        // Then
        verify(rabbitTemplate).convertAndSend(eq("test.request.queue"), any(Map.class));
    }

    @Test
    void testHandleResponse_WithValidRequestId() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("request_id", "test-123");
        response.put("success", true);

        // When
        rabbitMQClient.handleResponse(response);

        // Then - should not throw exception
        // The method handles unknown request IDs gracefully
    }

    @Test
    void testHandleResponse_WithNullRequestId() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        // When
        rabbitMQClient.handleResponse(response);

        // Then - should not throw exception
    }
}
