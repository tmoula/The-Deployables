package com.outreach.lead.infrastructure;

import org.springframework.stereotype.Component;
import com.outreach.lead.domain.Prospect;
import com.outreach.lead.domain.ProspectCriteria;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@Component
public class ProspectService {
    private final AiServiceClient aiServiceClient;
    private final RabbitMQClient rabbitMQClient;
    private final boolean useRabbitMQ;

    public ProspectService(AiServiceClient aiServiceClient, RabbitMQClient rabbitMQClient) {
        this.aiServiceClient = aiServiceClient;
        this.rabbitMQClient = rabbitMQClient;
        // Use RabbitMQ by default, fallback to HTTP if RabbitMQ is not available
        this.useRabbitMQ = true;
    }
    
    /**
     * Generate personalization hooks for a list of prospects
     * @param prospects List of prospects to generate hooks for
     * @param seller Seller profile information
     * @return List of prospects with personalization hooks
     */
    public List<Prospect> enrichProspectsWithHooks(List<Prospect> prospects, com.outreach.lead.domain.SellerProfile seller) {
        List<Prospect> enrichedProspects = new ArrayList<>();
        
        for (Prospect prospect : prospects) {
            try {
                System.out.println("Generating personalization hook for: " + prospect.company());
                String hook = aiServiceClient.generatePersonalizationHook(prospect, seller);
                
                // Create new prospect with hook
                Prospect enrichedProspect = new Prospect(
                    prospect.id(),
                    prospect.company(),
                    prospect.firstName(),
                    prospect.lastName(),
                    prospect.position(),
                    prospect.email(),
                    prospect.domain(),
                    prospect.industry(),
                    prospect.size(),
                    prospect.regions(),
                    prospect.stack(),
                    prospect.keywords(),
                    hook
                );
                
                enrichedProspects.add(enrichedProspect);
            } catch (Exception e) {
                System.err.println("Error generating hook for " + prospect.company() + ": " + e.getMessage());
                // Add prospect without hook if generation fails
                Prospect prospectWithoutHook = new Prospect(
                    prospect.id(),
                    prospect.company(),
                    prospect.firstName(),
                    prospect.lastName(),
                    prospect.position(),
                    prospect.email(),
                    prospect.domain(),
                    prospect.industry(),
                    prospect.size(),
                    prospect.regions(),
                    prospect.stack(),
                    prospect.keywords(),
                    "I noticed " + prospect.company() + " is in " + prospect.industry() + "."
                );
                enrichedProspects.add(prospectWithoutHook);
            }
        }
        
        return enrichedProspects;
    }

    /**
     * Search for prospects dynamically based on criteria using Gemini AI
     * @param criteria User-provided search criteria
     * @param limit Minimum number of companies to return (defaults to 10)
     * @return List of Prospect objects found
     */
    public List<Prospect> searchProspects(ProspectCriteria criteria, int limit) {
        List<Prospect> allProspects = new ArrayList<>();
        
        try {
            System.out.println("=== Starting prospect search (Gemini only) ===");
            System.out.println("Criteria: industry=" + criteria.industry() + ", minSize=" + criteria.minSize() + ", maxSize=" + criteria.maxSize());
            
            // Use AI Service to generate matching companies based on criteria
            // Always limit to maximum 5 companies
            int requestedCompanies = Math.min(limit, 5);
            if (limit > 5) {
                System.out.println("=== Requested limit " + limit + " exceeds maximum of 5, limiting to 5 ===");
            }
            System.out.println("=== Using AI Service to find matching companies ===");
            System.out.println("Requesting " + requestedCompanies + " companies from AI Service...");
            
            List<String> companyDomains;
            try {
                // Use RabbitMQ for lead generation
                // Note: This method is deprecated - use MatchService.startLeadGeneration instead
                // This is kept for backward compatibility but should not be called
                if (useRabbitMQ) {
                    System.out.println("=== WARNING: searchProspects is deprecated. Use MatchService.startLeadGeneration instead ===");
                    System.out.println("=== RabbitMQ async mode not supported in deprecated method. Falling back to HTTP ===");
                    // RabbitMQ is now async-only, so fall back to HTTP for this deprecated synchronous method
                    companyDomains = aiServiceClient.generateMatchingCompanies(criteria, requestedCompanies);
                } else {
                    System.out.println("=== Using HTTP to generate matching companies ===");
                    companyDomains = aiServiceClient.generateMatchingCompanies(criteria, requestedCompanies);
                }
                System.out.println("AI Service generated " + (companyDomains != null ? companyDomains.size() : 0) + " company domains");
                
                if (companyDomains == null || companyDomains.isEmpty()) {
                    System.err.println("ERROR: No companies generated by AI Service. Check your criteria or AI service connection.");
                    return allProspects;
                }
                
                System.out.println("Company domains from AI Service: " + companyDomains);
            } catch (Exception e) {
                System.err.println("ERROR calling AI Service: " + e.getMessage());
                e.printStackTrace();
                return allProspects;
            }
            
            // Process all companies generated
            int maxCompanies = companyDomains.size();
            System.out.println("Processing " + maxCompanies + " companies");
            
            // Create Prospect objects from Gemini-generated companies
            for (int i = 0; i < maxCompanies; i++) {
                String domain = companyDomains.get(i);
                if (domain == null || domain.isEmpty()) {
                    continue;
                }
                
                System.out.println("Creating prospect for company: " + domain);
                
                // Extract company name from domain (simple approach)
                String companyName = domain.split("\\.")[0];
                companyName = companyName.substring(0, 1).toUpperCase() + companyName.substring(1);
                
                // Generate realistic contact info based on domain and criteria
                String firstName = generateFirstName(companyName);
                String lastName = generateLastName(companyName);
                String position = generatePosition(criteria);
                String email = generateEmail(firstName, lastName, domain);
                
                // Create a Prospect with company information (hook will be added later)
                Prospect prospect = new Prospect(
                    UUID.randomUUID().toString(),  // id
                    companyName,                    // company name
                    firstName,                     // firstName
                    lastName,                      // lastName
                    position,                      // position
                    email,                         // email
                    domain,                        // domain
                    criteria.industry(),           // industry from criteria
                    criteria.maxSize() != null ? criteria.maxSize() : (criteria.minSize() != null ? criteria.minSize() : 100), // size
                    criteria.regions() != null ? criteria.regions() : List.of(), // regions
                    criteria.techUsed() != null ? criteria.techUsed() : List.of(), // stack
                    criteria.keywordMentions() != null ? criteria.keywordMentions() : List.of(), // keywords
                    null  // personalizationHook - will be generated later
                );
                
                allProspects.add(prospect);
                System.out.println("Created prospect: " + firstName + " " + lastName + " (" + position + ") at " + companyName);
            }
            
            System.out.println("Total prospects created: " + allProspects.size());
            
        } catch (Exception e) {
            System.err.println("Error in searchProspects(): " + e.getMessage());
            e.printStackTrace();
        }
        
        return allProspects;
    }
    
    /**
     * Generate a realistic first name based on company name
     */
    private String generateFirstName(String companyName) {
        String[] commonNames = {"Alex", "Jordan", "Taylor", "Morgan", "Casey", "Riley", "Avery", "Quinn", "Blake", "Cameron"};
        // Use company name hash to pick a consistent name
        int index = Math.abs(companyName.hashCode()) % commonNames.length;
        return commonNames[index];
    }
    
    /**
     * Generate a realistic last name based on company name
     */
    private String generateLastName(String companyName) {
        String[] commonLastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez"};
        // Use company name hash to pick a consistent name
        int index = Math.abs((companyName + "last").hashCode()) % commonLastNames.length;
        return commonLastNames[index];
    }
    
    /**
     * Generate a position based on criteria target roles
     */
    private String generatePosition(ProspectCriteria criteria) {
        if (criteria.targetRoles() != null && !criteria.targetRoles().isEmpty()) {
            // Use the first target role as the position
            return criteria.targetRoles().get(0);
        }
        // Default positions based on seniority level
        String seniority = criteria.seniorityLevel();
        if (seniority != null) {
            if (seniority.toLowerCase().contains("executive") || seniority.toLowerCase().contains("c-level")) {
                return "CEO";
            } else if (seniority.toLowerCase().contains("vp") || seniority.toLowerCase().contains("vice")) {
                return "VP of Operations";
            } else if (seniority.toLowerCase().contains("director")) {
                return "Director";
            } else if (seniority.toLowerCase().contains("manager")) {
                return "Manager";
            }
        }
        return "Decision Maker";
    }
    
    /**
     * Generate email address based on name and domain
     */
    private String generateEmail(String firstName, String lastName, String domain) {
        // Common email patterns
        String[] patterns = {
            firstName.toLowerCase() + "." + lastName.toLowerCase() + "@" + domain,
            firstName.toLowerCase() + lastName.toLowerCase() + "@" + domain,
            firstName.toLowerCase().charAt(0) + lastName.toLowerCase() + "@" + domain
        };
        // Use domain hash to pick a consistent pattern
        int index = Math.abs(domain.hashCode()) % patterns.length;
        return patterns[index];
    }
}

