package com.outreach.lead.infrastructure;

import com.outreach.lead.domain.entities.LeadBatchEntity;
import com.outreach.lead.domain.entities.LeadEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitMQResponseListenerTest {

    @Mock
    private RabbitMQClient rabbitMQClient;
    @Mock
    private LeadBatchRepository leadBatchRepository;
    @Mock
    private LeadRepository leadRepository;

    private RabbitMQResponseListener listener;

    @BeforeEach
    void setUp() {
        listener = new RabbitMQResponseListener(rabbitMQClient, leadBatchRepository, leadRepository);
    }

    @Test
    void testHandleLeadGenerationResponse_Success() {
        // Given
        LeadBatchEntity batch = new LeadBatchEntity();
        batch.setId(1);
        batch.setUserId(100);
        batch.setStatus("pending");

        when(leadBatchRepository.findById(1)).thenReturn(Optional.of(batch));
        when(leadRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> response = new HashMap<>();
        response.put("lead_batch_id", 1);
        response.put("success", true);
        response.put("company_domains", List.of("example.com", "test.com"));

        // When
        listener.handleLeadGenerationResponse(response);

        // Then
        verify(leadRepository).saveAll(anyList());
        verify(leadBatchRepository).save(batch);
        verify(rabbitMQClient).handleResponse(response);
    }

    @Test
    void testHandleLeadGenerationResponse_NoBatchId() {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        // When
        listener.handleLeadGenerationResponse(response);

        // Then
        verify(rabbitMQClient).handleResponse(response);
        verify(leadRepository, never()).saveAll(anyList());
    }

    @Test
    void testHandleLeadGenerationResponse_BatchNotFound() {
        // Given
        when(leadBatchRepository.findById(999)).thenReturn(Optional.empty());

        Map<String, Object> response = new HashMap<>();
        response.put("lead_batch_id", 999);
        response.put("success", true);

        // When
        listener.handleLeadGenerationResponse(response);

        // Then
        verify(rabbitMQClient).handleResponse(response);
        verify(leadRepository, never()).saveAll(anyList());
    }

    @Test
    void testHandleLeadGenerationResponse_ErrorResponse() {
        // Given
        LeadBatchEntity batch = new LeadBatchEntity();
        batch.setId(1);
        batch.setUserId(100);

        when(leadBatchRepository.findById(1)).thenReturn(Optional.of(batch));

        Map<String, Object> response = new HashMap<>();
        response.put("lead_batch_id", 1);
        response.put("success", false);
        response.put("error", "AI service error");

        // When
        listener.handleLeadGenerationResponse(response);

        // Then
        verify(leadBatchRepository).save(batch);
        verify(rabbitMQClient).handleResponse(response);
        verify(leadRepository, never()).saveAll(anyList());
    }
}
