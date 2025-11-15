package com.outreach.lead.application;

import com.outreach.lead.domain.Prospect;
import com.outreach.lead.domain.ProspectCriteria;
import com.outreach.lead.domain.SellerProfile;
import org.springframework.stereotype.Service;
import com.outreach.lead.infrastructure.ProspectService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;



@Service
public class MatchService {
    private SellerProfile seller;                       // set via PUT /seller
    private final ProspectService prospectService;
    
    public MatchService(ProspectService prospectService) {
        this.prospectService = prospectService;
        // No hardcoded data - prospects are fetched dynamically based on user criteria
    }
    

    // 1) Save & get seller
    public SellerProfile getSeller(){ return seller; }
    public SellerProfile setSeller(SellerProfile s){ this.seller = s; return s; }

    // 2) DB listing/adding (admin/debug; UI won't input names)
    public List<Prospect> listProspects(){ 
        // Return empty list - prospects are now fetched dynamically
        return List.of(); 
    }
    public Prospect createProspect(Prospect in){ 
        // This method is kept for API compatibility but doesn't store prospects
        // Prospects are now fetched dynamically from Gemini AI
        if(in == null) throw new IllegalArgumentException("Prospect cannot be null");
        // Auto-generate ID if not provided
        return in.id() == null || in.id().isEmpty() 
            ? new Prospect(String.valueOf(System.currentTimeMillis()), in.company(), in.firstName(), in.lastName(),
                          in.position(), in.email(), in.domain(), in.industry(), in.size(), 
                          in.regions(), in.stack(), in.keywords(), in.personalizationHook())
            : in;
    }


    // 3) Match using criteria - dynamically fetch from Gemini AI
    public List<ScoredProspect> match(ProspectCriteria c, int limit){
        System.out.println("=== MATCH SERVICE: Starting match ===");
        System.out.println("Seller is null: " + (seller == null));
        if (seller == null) {
            System.err.println("ERROR: Seller profile is null. Cannot match without seller profile.");
            return List.of();
        }

        // Dynamically fetch prospects from Gemini AI based on criteria
        // For testing: limit to 5 prospects
        int requestedLimit = 5;
        System.out.println("=== MATCH SERVICE: Calling prospectService.searchProspects ===");
        List<Prospect> fetchedProspects = prospectService.searchProspects(c, requestedLimit);
        System.out.println("=== MATCH SERVICE: Received " + fetchedProspects.size() + " prospects from searchProspects ===");
        
        if (fetchedProspects.isEmpty()) {
            System.err.println("ERROR: No prospects fetched from Gemini. Check API key and criteria.");
            return List.of();
        }
        
        // Generate personalization hooks for all prospects using Gemini AI
        System.out.println("=== Generating personalization hooks for " + fetchedProspects.size() + " prospects ===");
        fetchedProspects = prospectService.enrichProspectsWithHooks(fetchedProspects, seller);

        // (a) FILTER by criteria (additional filtering on fetched prospects)
        var filtered = fetchedProspects.stream().filter(p -> {
            // Company name filter
            if (c.companyName()!=null && !c.companyName().isEmpty() && 
                p.company()!=null && !p.company().toLowerCase().contains(c.companyName().toLowerCase())) 
                return false;
            
            // Domain filter
            if (c.domain()!=null && !c.domain().isEmpty() && 
                p.domain()!=null && !p.domain().toLowerCase().contains(c.domain().toLowerCase())) 
                return false;
            
            // Industry filter
            if (c.industry()!=null && p.industry()!=null &&
                    !p.industry().equalsIgnoreCase(c.industry())) return false;

            // Size range filter
            if (c.minSize()!=null && (p.size()==null || p.size()<c.minSize())) return false;
            if (c.maxSize()!=null && (p.size()==null || p.size()>c.maxSize())) return false;

            // Region filter (check both regions and headquartersRegion)
            if (c.regions()!=null && !c.regions().isEmpty()) {
                if (p.regions()==null || p.regions().stream().noneMatch(c.regions()::contains)) return false;
            }
            if (c.headquartersRegion()!=null && !c.headquartersRegion().isEmpty()) {
                if (p.regions()==null || p.regions().stream().noneMatch(r -> r.toLowerCase().contains(c.headquartersRegion().toLowerCase()))) return false;
            }
            
            // Technology stack filter (check both techUsed and requiredStack for backward compatibility)
            var techFilter = c.techUsed()!=null && !c.techUsed().isEmpty() ? c.techUsed() : 
                           (c.requiredStack()!=null && !c.requiredStack().isEmpty() ? c.requiredStack() : null);
            if (techFilter!=null && (p.stack()==null || !p.stack().containsAll(techFilter))) return false;
            
            // keywords are "desired", not required → don't filter here
            return true;
        });

        // (b) SCORE the filtered set using seller + criteria
        return filtered
                .map(p -> new ScoredProspect(p, score(seller, c, p)))
                .sorted(Comparator.comparingDouble(ScoredProspect::score).reversed())
                .limit(Math.max(1, limit))
                .toList();
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
