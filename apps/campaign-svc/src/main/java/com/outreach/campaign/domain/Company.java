package com.outreach.campaign.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "companies")
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_id")
    private Integer companyId;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "website")
    private String website;
    
    @Column(name = "industry")
    private String industry;
    
    @Column(name = "employee_count")
    private Integer employeeCount;
    
    @Column(name = "hq_location")
    private String hqLocation;
    
    @Column(name = "enrichment_notes", columnDefinition = "TEXT")
    private String enrichmentNotes;
    
    // Getters and setters
    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
    public Integer getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(Integer employeeCount) { this.employeeCount = employeeCount; }
    public String getHqLocation() { return hqLocation; }
    public void setHqLocation(String hqLocation) { this.hqLocation = hqLocation; }
    public String getEnrichmentNotes() { return enrichmentNotes; }
    public void setEnrichmentNotes(String enrichmentNotes) { this.enrichmentNotes = enrichmentNotes; }
}

