package com.outreach.lead.infrastructure;

import com.outreach.lead.domain.entities.LeadBatchEntity;
import com.outreach.lead.domain.entities.LeadEntity;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RabbitMQResponseListener {
    private final RabbitMQClient rabbitMQClient;
    private final LeadBatchRepository leadBatchRepository;
    private final LeadRepository leadRepository;
    
    public RabbitMQResponseListener(
        RabbitMQClient rabbitMQClient,
        LeadBatchRepository leadBatchRepository,
        LeadRepository leadRepository
    ) {
        this.rabbitMQClient = rabbitMQClient;
        this.leadBatchRepository = leadBatchRepository;
        this.leadRepository = leadRepository;
    }
    
    @RabbitListener(queues = "${rabbitmq.lead.response.queue:ai.leads.generation.responses}")
    @Transactional
    public void handleLeadGenerationResponse(Map<String, Object> response) {
        System.out.println("=== RABBITMQ RESPONSE LISTENER: Received response ===");
        System.out.println("Response: " + response);
        System.out.println("Response keys: " + (response != null ? response.keySet() : "null"));
        System.out.println("Response type: " + (response != null ? response.getClass().getName() : "null"));
        
        // Extract batch_id from response
        Integer batchId = null;
        Object batchIdObj = response.get("lead_batch_id");
        if (batchIdObj instanceof Integer) {
            batchId = (Integer) batchIdObj;
        } else if (batchIdObj instanceof Number) {
            batchId = ((Number) batchIdObj).intValue();
        }
        
        if (batchId == null) {
            System.err.println("=== ERROR: No batch_id in response, cannot save leads ===");
            rabbitMQClient.handleResponse(response);
            return;
        }
        
        // Get batch to get user_id
        LeadBatchEntity batch = leadBatchRepository.findById(batchId).orElse(null);
        if (batch == null) {
            System.err.println("=== ERROR: Batch not found: " + batchId + " ===");
            rabbitMQClient.handleResponse(response);
            return;
        }
        
        // Extract company domains from response
        if (Boolean.TRUE.equals(response.get("success"))) {
            @SuppressWarnings("unchecked")
            List<String> domains = (List<String>) response.get("company_domains");
            
            if (domains != null && !domains.isEmpty()) {
                System.out.println("=== Saving " + domains.size() + " leads to database for batch: " + batchId + " ===");
                
                // Create LeadEntity for each domain
                List<LeadEntity> leads = new ArrayList<>();
                for (String domain : domains) {
                    LeadEntity lead = createLeadFromDomain(domain, batch.getUserId(), batchId);
                    leads.add(lead);
                }
                
                // Batch save leads
                leadRepository.saveAll(leads);
                
                // Update batch status
                batch.setStatus("ready");
                batch.setTotalLeads(leads.size());
                leadBatchRepository.save(batch);
                
                System.out.println("=== Successfully saved " + leads.size() + " leads to database ===");
            } else {
                System.err.println("=== WARNING: No company domains in response ===");
                batch.setStatus("failed");
                batch.setErrorMessage("No company domains generated");
                leadBatchRepository.save(batch);
            }
        } else {
            // Error case
            String error = (String) response.get("error");
            System.err.println("=== ERROR in lead generation: " + error + " ===");
            batch.setStatus("failed");
            batch.setErrorMessage(error);
            leadBatchRepository.save(batch);
        }
        
        // Complete the future for the original request
        rabbitMQClient.handleResponse(response);
    }
    
    /**
     * Create a LeadEntity from a company domain
     */
    private LeadEntity createLeadFromDomain(String domain, Integer userId, Integer batchId) {
        LeadEntity lead = new LeadEntity();
        lead.setUserId(userId);
        lead.setBatchId(batchId);
        lead.setCompanyWebsite(domain);
        
        // Extract company name from domain
        String companyName = domain.split("\\.")[0];
        companyName = companyName.substring(0, 1).toUpperCase() + companyName.substring(1);
        lead.setCompanyName(companyName);
        
        // Generate placeholder contact info (can be enriched later)
        lead.setFirstName("John");
        lead.setLastName("Doe");
        lead.setJobTitle("CEO");
        lead.setEmail("contact@" + domain);
        
        return lead;
    }
}

