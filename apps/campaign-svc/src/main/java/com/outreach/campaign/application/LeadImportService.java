package com.outreach.campaign.application;

import com.outreach.campaign.domain.Lead;
import com.outreach.campaign.domain.Company;
import com.outreach.campaign.domain.Contact;
import com.outreach.campaign.infrastructure.CompanyRepository;
import com.outreach.campaign.infrastructure.ContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class LeadImportService {
    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    
    public LeadImportService(
        CompanyRepository companyRepository,
        ContactRepository contactRepository
    ) {
        this.companyRepository = companyRepository;
        this.contactRepository = contactRepository;
    }
    
    @Transactional
    public List<Map<String, Object>> importLeadsToDatabase(List<Lead> leads, Integer campaignId) {
        List<Map<String, Object>> importedLeads = new ArrayList<>();
        Map<String, Company> companyCache = new HashMap<>(); // Cache by company name
        
        for (Lead lead : leads) {
            try {
                // Find or create company
                Company company = companyCache.get(lead.company());
                if (company == null) {
                    // Try to find existing company by name
                    List<Company> existingCompanies = companyRepository.findAll();
                    company = existingCompanies.stream()
                        .filter(c -> c.getName() != null && c.getName().equalsIgnoreCase(lead.company()))
                        .findFirst()
                        .orElse(null);
                    
                    if (company == null) {
                        // Create new company
                        company = new Company();
                        company.setName(lead.company());
                        company.setWebsite(lead.domain());
                        company.setIndustry(lead.industry());
                        company.setEmployeeCount(lead.companySize());
                        // Combine notes and personalization hook for enrichment
                        String enrichmentNotes = "";
                        if (lead.notes() != null && !lead.notes().isEmpty()) {
                            enrichmentNotes = lead.notes();
                        }
                        if (lead.personalizationHook() != null && !lead.personalizationHook().isEmpty()) {
                            if (!enrichmentNotes.isEmpty()) enrichmentNotes += " | ";
                            enrichmentNotes += "Hook: " + lead.personalizationHook();
                        }
                        company.setEnrichmentNotes(enrichmentNotes.isEmpty() ? null : enrichmentNotes);
                        company = companyRepository.save(company);
                    } else {
                        // Update existing company with new info if available
                        if (lead.industry() != null && company.getIndustry() == null) {
                            company.setIndustry(lead.industry());
                        }
                        if (lead.companySize() != null && company.getEmployeeCount() == null) {
                            company.setEmployeeCount(lead.companySize());
                        }
                        if (lead.notes() != null && !lead.notes().isEmpty()) {
                            String existingNotes = company.getEnrichmentNotes() != null ? company.getEnrichmentNotes() : "";
                            if (!existingNotes.contains(lead.notes())) {
                                company.setEnrichmentNotes(existingNotes.isEmpty() ? lead.notes() : existingNotes + " | " + lead.notes());
                            }
                        }
                        company = companyRepository.save(company);
                    }
                    companyCache.put(lead.company(), company);
                }
                
                // Create contact
                Contact contact = new Contact();
                contact.setCompanyId(company.getCompanyId());
                contact.setFirstName(lead.firstName());
                contact.setLastName(lead.lastName());
                contact.setJobTitle(lead.position());
                contact.setEmail(lead.email());
                contact.setPersonalizationNotes(lead.personalizationHook());
                contact = contactRepository.save(contact);
                
                // Return mapping info
                Map<String, Object> leadInfo = new HashMap<>();
                leadInfo.put("leadId", lead.id());
                leadInfo.put("companyId", company.getCompanyId());
                leadInfo.put("contactId", contact.getContactId());
                leadInfo.put("companyName", company.getName());
                leadInfo.put("contactName", contact.getFirstName() + " " + (contact.getLastName() != null ? contact.getLastName() : ""));
                importedLeads.add(leadInfo);
                
            } catch (Exception e) {
                System.err.println("Error importing lead " + lead.company() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return importedLeads;
    }
}

