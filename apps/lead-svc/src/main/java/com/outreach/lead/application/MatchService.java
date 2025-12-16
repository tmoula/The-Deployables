package com.outreach.lead.application;

import com.outreach.lead.api.DTO.LeadBatchResponse;
import com.outreach.lead.domain.Prospect;
import com.outreach.lead.domain.ProspectCriteria;
import com.outreach.lead.domain.SellerProfile;
import com.outreach.lead.domain.entities.*;
import com.outreach.lead.infrastructure.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class MatchService {
    // private SellerProfile seller; // REMOVED: Now stateless, stored in DB
    private final ProspectService prospectService;
    private final SenderCompanyRepository senderCompanyRepository;
    private final ICPProfileRepository icpProfileRepository;
    private final LeadBatchRepository leadBatchRepository;
    private final RabbitMQClient rabbitMQClient;
    private final UserContextService userContextService;
    private final ObjectMapper objectMapper;
    
    public MatchService(
        ProspectService prospectService,
        SenderCompanyRepository senderCompanyRepository,
        ICPProfileRepository icpProfileRepository,
        LeadBatchRepository leadBatchRepository,
        RabbitMQClient rabbitMQClient,
        UserContextService userContextService,
        ObjectMapper objectMapper
    ) {
        this.prospectService = prospectService;
        this.senderCompanyRepository = senderCompanyRepository;
        this.icpProfileRepository = icpProfileRepository;
        this.leadBatchRepository = leadBatchRepository;
        this.rabbitMQClient = rabbitMQClient;
        this.userContextService = userContextService;
        this.objectMapper = objectMapper;
    }
    



    // 1) Save & get seller (Stateless - stored in DB description field)
    public SellerProfile getSeller(Integer userId) { 
        if (userId == null) return null;
        return senderCompanyRepository.findByUserId(userId)
            .map(entity -> {
                String potentialJson = entity.getDescription();
                if (potentialJson == null || potentialJson.isEmpty()) return null;
                
                // Try to parse as JSON first
                try {
                    return objectMapper.readValue(potentialJson, SellerProfile.class);
                } catch (JsonProcessingException e) {
                    // Not JSON - treat as legacy text description
                    // We can construct a partial profile or just return null
                    // For now, let's treat it as null so user re-saves the full profile
                    System.err.println("Description is not valid JSON profile (likely legacy text): " + e.getMessage());
                    return null;
                }
            })
            .orElse(null);
    }

    public SellerProfile setSeller(SellerProfile s, Integer userId) {
        if (userId == null) throw new IllegalArgumentException("User ID required to set seller profile");
        
        try {
            // Serialize full profile to JSON
            String json = objectMapper.writeValueAsString(s);
            
            SenderCompanyEntity entity = senderCompanyRepository.findByUserId(userId)
                .orElse(new SenderCompanyEntity());
            
            entity.setUserId(userId);
            entity.setName(s.companyName());
            entity.setIndustry(s.industry());
            
            // MAGIC: Store the JSON in the description field (TEXT column)
            entity.setDescription(json);
            
            senderCompanyRepository.save(entity);
            return s;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize seller profile", e);
        }
    }

    // 2) DB listing/adding (admin/debug; UI won't input names)
    public List<Prospect> listProspects(){ 
        // Return empty list - prospects are now fetched dynamically
        return List.of(); 
    }
    public Prospect createProspect(Prospect in){ 
        // This method is kept for API compatibility but doesn't store prospects
        // Prospects are now fetched dynamically from AI Service
        if(in == null) throw new IllegalArgumentException("Prospect cannot be null");
        // Auto-generate ID if not provided
        return in.id() == null || in.id().isEmpty() 
            ? new Prospect(String.valueOf(System.currentTimeMillis()), in.company(), in.firstName(), in.lastName(),
                          in.position(), in.email(), in.domain(), in.industry(), in.size(), 
                          in.regions(), in.stack(), in.keywords(), in.personalizationHook())
            : in;
    }


    /**
     * Start lead generation process: save sender company, ICP profile, create batch, and publish to RabbitMQ
     * Returns immediately with batch_id for async processing
     */
    @Transactional
    public LeadBatchResponse startLeadGeneration(SellerProfile sellerProfile, ProspectCriteria criteria, int limit, Integer userId) {
        System.out.println("=== MATCH SERVICE: Starting lead generation for user_id: " + userId + " ===");
        
        // 1. Save or update sender company
        SenderCompanyEntity senderCompany = saveOrUpdateSenderCompany(userId, sellerProfile);
        System.out.println("=== Saved/Updated sender company: " + senderCompany.getId() + " ===");
        
        // 2. Save or update ICP profile
        ICPProfileEntity icpProfile = saveOrUpdateICPProfile(userId, criteria);
        System.out.println("=== Saved/Updated ICP profile: " + icpProfile.getId() + " ===");
        
        // 3. Create lead batch
        LeadBatchEntity batch = new LeadBatchEntity();
        batch.setUserId(userId);
        batch.setIcpId(icpProfile.getId());
        batch.setSource("ai_scraper");
        batch.setStatus("running");
        batch.setRequestedLeadCount(Math.min(limit, 5));
        batch.setTotalLeads(0);
        batch = leadBatchRepository.save(batch);
        System.out.println("=== Created lead batch: " + batch.getId() + " ===");
        
        // 4. Publish to RabbitMQ (async processing)
        try {
            rabbitMQClient.generateMatchingCompanies(criteria, Math.min(limit, 5), batch.getId());
            System.out.println("=== Published lead generation request to RabbitMQ for batch: " + batch.getId() + " ===");
        } catch (Exception e) {
            System.err.println("=== ERROR publishing to RabbitMQ: " + e.getMessage() + " ===");
            batch.setStatus("failed");
            batch.setErrorMessage("Failed to publish to RabbitMQ: " + e.getMessage());
            leadBatchRepository.save(batch);
            throw new RuntimeException("Failed to start lead generation", e);
        }
        
        return new LeadBatchResponse(batch.getId(), "running", "Lead generation started");
    }
    
    /**
     * Save or update sender company (upsert by user_id)
     */
    private SenderCompanyEntity saveOrUpdateSenderCompany(Integer userId, SellerProfile seller) {
        Optional<SenderCompanyEntity> existing = senderCompanyRepository.findByUserId(userId);
        
        SenderCompanyEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
        } else {
            entity = new SenderCompanyEntity();
            entity.setUserId(userId);
        }
        
        entity.setName(seller.companyName());
        entity.setIndustry(seller.industry());
        // Map other fields as needed
        if (seller.headquartersRegion() != null) {
            // Could store in description or add a field
            entity.setDescription("Headquarters: " + seller.headquartersRegion());
        }
        
        return senderCompanyRepository.save(entity);
    }
    
    /**
     * Save or update ICP profile (upsert by user_id + criteria hash)
     */
    private ICPProfileEntity saveOrUpdateICPProfile(Integer userId, ProspectCriteria criteria) {
        // Try to find existing ICP profile with same criteria
        Optional<ICPProfileEntity> existing = icpProfileRepository.findByUserIdAndTargetIndustryAndCompanySizeMinAndCompanySizeMax(
            userId,
            criteria.industry(),
            criteria.minSize(),
            criteria.maxSize()
        );
        
        ICPProfileEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
        } else {
            entity = new ICPProfileEntity();
            entity.setUserId(userId);
            // Generate name from criteria
            String name = (criteria.industry() != null ? criteria.industry() : "General") + " - " +
                         (criteria.minSize() != null ? criteria.minSize() : "0") + "-" +
                         (criteria.maxSize() != null ? criteria.maxSize() : "∞") + " employees";
            entity.setName(name);
        }
        
        entity.setTargetIndustry(criteria.industry());
        entity.setCompanySizeMin(criteria.minSize());
        entity.setCompanySizeMax(criteria.maxSize());
        
        // Build target titles from criteria
        if (criteria.targetRoles() != null && !criteria.targetRoles().isEmpty()) {
            entity.setTargetTitles(String.join(", ", criteria.targetRoles()));
        }
        
        // Build geo region
        if (criteria.regions() != null && !criteria.regions().isEmpty()) {
            entity.setGeoRegion(String.join(", ", criteria.regions()));
        } else if (criteria.headquartersRegion() != null) {
            entity.setGeoRegion(criteria.headquartersRegion());
        }
        
        return icpProfileRepository.save(entity);
    }

    // Legacy method for backward compatibility (returns empty list, use startLeadGeneration instead)
    public List<ScoredProspect> match(ProspectCriteria c, int limit){
        System.out.println("=== MATCH SERVICE: Legacy match() called - returning empty list ===");
        System.out.println("=== Use startLeadGeneration() instead for database persistence ===");
        return List.of();
    }

    // simple, explainable scoring 0..100 (weights are tunable)
    private double score(SellerProfile s, ProspectCriteria c, Prospect p){
        double total = 0;

        // Industry alignment (from seller, if present)
        if (s.industry()!=null && p.industry()!=null &&
                s.industry().equalsIgnoreCase(p.industry())) total += 30;

        // Region overlap (seller target regions vs prospect)
        if (s.targetRegions()!=null && p.regions()!=null) {
            long overlap = p.regions().stream().filter(s.targetRegions()::contains).count();
            if (overlap > 0) total += 15;
        }

        // Size proximity to seller’s size (closer is better)
        if (s.companySize()!=null && p.size()!=null) {
            int diff = Math.abs(s.companySize() - p.size());
            total += Math.max(0, 15 - (diff / 300.0)); // within ~4500 employees still gets some points
        }

        // Criteria-desired keywords (nice-to-have) - check both desiredKeywords and keywordMentions
        var desiredKw = new ArrayList<String>();
        if (c.desiredKeywords()!=null) desiredKw.addAll(c.desiredKeywords());
        if (c.keywordMentions()!=null) desiredKw.addAll(c.keywordMentions());
        
        var prospectTerms = new ArrayList<String>();
        if (p.keywords()!=null) prospectTerms.addAll(p.keywords());
        if (p.stack()!=null) prospectTerms.addAll(p.stack());
        long kwOverlap = prospectTerms.stream().map(String::toLowerCase)
                .filter(t -> desiredKw.stream().map(String::toLowerCase).anyMatch(t::contains))
                .count();
        total += Math.min(20, kwOverlap * 10.0);

        // Seller value-prop vs prospect keywords/stack
        var vp = s.valuePropositionKeywords()==null ? List.<String>of() : s.valuePropositionKeywords();
        long vpOverlap = prospectTerms.stream().map(String::toLowerCase)
                .filter(t -> vp.stream().map(String::toLowerCase).anyMatch(t::contains))
                .count();
        total += Math.min(20, vpOverlap * 10.0);

        return Math.min(100, total);
    }

    public record ScoredProspect(Prospect prospect, double score) {}
}
