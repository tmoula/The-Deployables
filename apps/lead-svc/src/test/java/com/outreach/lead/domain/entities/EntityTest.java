package com.outreach.lead.domain.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EntityTest {

    @Test
    void testLeadEntityGettersAndSetters() {
        LeadEntity lead = new LeadEntity();
        lead.setId(1);
        lead.setUserId(100);
        lead.setBatchId(200);
        lead.setFirstName("John");
        lead.setLastName("Doe");
        lead.setEmail("john@example.com");
        lead.setCompanyName("Example Inc");
        lead.setCompanyWebsite("example.com");
        lead.setJobTitle("CEO");
        lead.setLinkedinUrl("https://linkedin.com/in/john");
        lead.setCompanyLinkedin("https://linkedin.com/company/example");
        lead.setCountry("US");
        lead.setCustomNotes("Test notes");
        lead.setCreatedAt(LocalDateTime.now());

        assertEquals(1, lead.getId());
        assertEquals(100, lead.getUserId());
        assertEquals(200, lead.getBatchId());
        assertEquals("John", lead.getFirstName());
        assertEquals("Doe", lead.getLastName());
        assertEquals("john@example.com", lead.getEmail());
        assertEquals("Example Inc", lead.getCompanyName());
        assertEquals("example.com", lead.getCompanyWebsite());
        assertEquals("CEO", lead.getJobTitle());
        assertEquals("https://linkedin.com/in/john", lead.getLinkedinUrl());
        assertEquals("https://linkedin.com/company/example", lead.getCompanyLinkedin());
        assertEquals("US", lead.getCountry());
        assertEquals("Test notes", lead.getCustomNotes());
        assertNotNull(lead.getCreatedAt());
    }

    @Test
    void testLeadBatchEntityGettersAndSetters() {
        LeadBatchEntity batch = new LeadBatchEntity();
        batch.setId(1);
        batch.setUserId(100);
        batch.setStatus("pending");
        batch.setTotalLeads(10);
        batch.setErrorMessage("Test error");
        batch.setCreatedAt(LocalDateTime.now());

        assertEquals(1, batch.getId());
        assertEquals(100, batch.getUserId());
        assertEquals("pending", batch.getStatus());
        assertEquals(10, batch.getTotalLeads());
        assertEquals("Test error", batch.getErrorMessage());
        assertNotNull(batch.getCreatedAt());
    }

    @Test
    void testICPProfileEntityGettersAndSetters() {
        ICPProfileEntity icp = new ICPProfileEntity();
        icp.setId(1);
        icp.setUserId(100);
        icp.setName("Tech ICP");
        icp.setTargetIndustry("Technology");
        icp.setTargetTitles("CTO,VP Engineering");
        icp.setCompanySizeMin(50);
        icp.setCompanySizeMax(500);
        icp.setGeoRegion("US,EU");
        icp.setPainPoints("Scalability,Performance");
        icp.setCreatedAt(LocalDateTime.now());

        assertEquals(1, icp.getId());
        assertEquals(100, icp.getUserId());
        assertEquals("Tech ICP", icp.getName());
        assertEquals("Technology", icp.getTargetIndustry());
        assertEquals("CTO,VP Engineering", icp.getTargetTitles());
        assertEquals(50, icp.getCompanySizeMin());
        assertEquals(500, icp.getCompanySizeMax());
        assertEquals("US,EU", icp.getGeoRegion());
        assertEquals("Scalability,Performance", icp.getPainPoints());
        assertNotNull(icp.getCreatedAt());
    }

    @Test
    void testSenderCompanyEntityGettersAndSetters() {
        SenderCompanyEntity sender = new SenderCompanyEntity();
        sender.setId(1);
        sender.setUserId(100);
        sender.setName("My Company");
        sender.setWebsite("https://mycompany.com");
        sender.setIndustry("Technology");
        sender.setDescription("AI Solutions Provider");
        sender.setLogoUrl("https://mycompany.com/logo.png");
        sender.setCreatedAt(LocalDateTime.now());

        assertEquals(1, sender.getId());
        assertEquals(100, sender.getUserId());
        assertEquals("My Company", sender.getName());
        assertEquals("https://mycompany.com", sender.getWebsite());
        assertEquals("Technology", sender.getIndustry());
        assertEquals("AI Solutions Provider", sender.getDescription());
        assertEquals("https://mycompany.com/logo.png", sender.getLogoUrl());
        assertNotNull(sender.getCreatedAt());
    }

    @Test
    void testUserSummaryGettersAndSetters() {
        UserSummary user = new UserSummary();
        user.setId(1);
        user.setEmail("user@example.com");

        assertEquals(1, user.getId());
        assertEquals("user@example.com", user.getEmail());
    }
}
