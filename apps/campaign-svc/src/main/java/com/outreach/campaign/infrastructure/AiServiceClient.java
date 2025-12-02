package com.outreach.campaign.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.HashMap;

@Component
public class AiServiceClient {
    private final String aiServiceUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiServiceClient(
        @Value("${ai.service.url:http://ai-svc:8090}") String aiServiceUrl
    ) {
        this.aiServiceUrl = aiServiceUrl;
        this.restTemplate = new RestTemplate();
    }

    public Map<String, Object> generateEmail(Map<String, Object> requestBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            
            String url = aiServiceUrl + "/api/v1/emails/generate";
            
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                (Class<Map<String, Object>>) (Class<?>) Map.class
            );
            
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new RuntimeException("AI Service returned null response");
            }
            
            Object errorObj = responseBody.get("error");
            if (errorObj != null) {
                throw new RuntimeException("AI Service Error: " + errorObj.toString());
            }
            
            return responseBody;
        } catch (Exception e) {
            throw new RuntimeException("Error calling AI Service: " + e.getMessage(), e);
        }
    }
}

