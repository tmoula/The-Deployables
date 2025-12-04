package com.outreach.campaign.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "contacts")
public class Contact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contact_id")
    private Integer contactId;
    
    @Column(name = "company_id")
    private Integer companyId;
    
    @Column(name = "first_name")
    private String firstName;
    
    @Column(name = "last_name")
    private String lastName;
    
    @Column(name = "job_title")
    private String jobTitle;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "personalization_notes", columnDefinition = "TEXT")
    private String personalizationNotes;
    
    // Getters and setters
    public Integer getContactId() { return contactId; }
    public void setContactId(Integer contactId) { this.contactId = contactId; }
    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPersonalizationNotes() { return personalizationNotes; }
    public void setPersonalizationNotes(String personalizationNotes) { this.personalizationNotes = personalizationNotes; }
}




