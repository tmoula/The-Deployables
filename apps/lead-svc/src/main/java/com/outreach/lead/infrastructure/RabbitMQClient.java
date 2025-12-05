package com.outreach.lead.infrastructure;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.outreach.lead.domain.ProspectCriteria;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class RabbitMQClient {
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String leadRequestQueue;
    private final String leadResponseQueue;
    private final Map<String, CompletableFuture<Map<String, Object>>> pendingRequests = new ConcurrentHashMap<>();
    
    public RabbitMQClient(
        RabbitTemplate rabbitTemplate,
        @Value("${rabbitmq.lead.request.queue:ai.leads.generation.requests}") String leadRequestQueue,
        @Value("${rabbitmq.lead.response.queue:ai.leads.generation.responses}") String leadResponseQueue
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = new ObjectMapper();
        this.leadRequestQueue = leadRequestQueue;
        this.leadResponseQueue = leadResponseQueue;
        
        // Configure message converter
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        rabbitTemplate.setReceiveTimeout(60000); // 60 seconds timeout for receiving
    }
    
    /**
     * Send lead generation request to RabbitMQ and wait for response
     * @param criteria Prospect search criteria
     * @param maxCompanies Maximum number of companies to return
     * @param batchId Lead batch ID to associate with this request
     * @return List of company domains
     */
    public List<String> generateMatchingCompanies(ProspectCriteria criteria, int maxCompanies, Integer batchId) {
        try {
            // Generate unique request ID
            String requestId = UUID.randomUUID().toString();
            
            // Build request message
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("request_id", requestId);
            requestBody.put("max_companies", maxCompanies);
            
            // Build criteria map
            Map<String, Object> criteriaMap = new HashMap<>();
            if (criteria.industry() != null) criteriaMap.put("industry", criteria.industry());
            if (criteria.minSize() != null) criteriaMap.put("min_size", criteria.minSize());
            if (criteria.maxSize() != null) criteriaMap.put("max_size", criteria.maxSize());
            if (criteria.hqCountry() != null) criteriaMap.put("hq_country", criteria.hqCountry());
            if (criteria.headquartersRegion() != null) criteriaMap.put("headquarters_region", criteria.headquartersRegion());
            if (criteria.regions() != null) criteriaMap.put("regions", criteria.regions());
            if (criteria.techUsed() != null) criteriaMap.put("tech_used", criteria.techUsed());
            if (criteria.keywordMentions() != null) criteriaMap.put("keyword_mentions", criteria.keywordMentions());
            if (criteria.targetRoles() != null) criteriaMap.put("target_roles", criteria.targetRoles());
            if (criteria.seniorityLevel() != null) criteriaMap.put("seniority_level", criteria.seniorityLevel());
            
            requestBody.put("criteria", criteriaMap);
            
            // Include batch_id in request for response handler
            if (batchId != null) {
                requestBody.put("lead_batch_id", batchId);
            }
            
            System.out.println("=== RABBITMQ CLIENT: Publishing lead generation request ===");
            System.out.println("Request ID: " + requestId);
            System.out.println("Batch ID: " + batchId);
            System.out.println("Queue: " + leadRequestQueue);
            System.out.println("Request body: " + objectMapper.writeValueAsString(requestBody));
            
            // Create future for response
            CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
            pendingRequests.put(requestId, future);
            
            // Publish request
            rabbitTemplate.convertAndSend(leadRequestQueue, requestBody);
            
            System.out.println("=== RABBITMQ CLIENT: Waiting for response (timeout: 60s) ===");
            
            // Wait for response (with timeout)
            Map<String, Object> response = future.get(60, TimeUnit.SECONDS);
            pendingRequests.remove(requestId);
            
            System.out.println("=== RABBITMQ CLIENT: Received response ===");
            System.out.println("Response: " + objectMapper.writeValueAsString(response));
            
            // Extract company domains
            if (Boolean.TRUE.equals(response.get("success"))) {
                @SuppressWarnings("unchecked")
                List<String> domains = (List<String>) response.get("company_domains");
                if (domains != null) {
                    System.out.println("AI Service generated " + domains.size() + " company domains via RabbitMQ: " + domains);
                    return domains;
                }
            } else {
                String error = (String) response.get("error");
                System.err.println("=== RABBITMQ CLIENT ERROR ===");
                System.err.println("Error: " + error);
                throw new RuntimeException("AI Service Error: " + error);
            }
            
            return List.of();
            
        } catch (java.util.concurrent.TimeoutException e) {
            System.err.println("=== RABBITMQ CLIENT: Timeout waiting for response ===");
            throw new RuntimeException("Timeout waiting for AI service response via RabbitMQ", e);
        } catch (Exception e) {
            System.err.println("Error calling AI Service via RabbitMQ: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
    
    /**
     * Handle response message from RabbitMQ
     * This is called by RabbitMQResponseListener
     */
    public void handleResponse(Map<String, Object> response) {
        String requestId = (String) response.get("request_id");
        if (requestId != null && pendingRequests.containsKey(requestId)) {
            CompletableFuture<Map<String, Object>> future = pendingRequests.remove(requestId);
            if (future != null) {
                future.complete(response);
                System.out.println("=== RABBITMQ CLIENT: Completed future for request_id: " + requestId + " ===");
            }
        } else {
            System.err.println("=== RABBITMQ CLIENT: Received response for unknown request_id: " + requestId + " ===");
        }
    }
}

