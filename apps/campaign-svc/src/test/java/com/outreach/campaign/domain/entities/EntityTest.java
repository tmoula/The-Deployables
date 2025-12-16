package com.outreach.campaign.domain.entities;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;

class EntityTest {

    @Test
    void testCampaignEntity() {
        CampaignEntity e = new CampaignEntity();
        e.setId(1);
        e.setUserId(2);
        e.setName("Name");
        e.setStatus("running");
        e.setCsvFilename("file.csv");
        e.setCsvColumns("[]");
        e.setEmailSubject("Subj");
        e.setEmailBody("Body");
        e.setStartAt(LocalDateTime.now());
        e.setCreatedAt(LocalDateTime.now());
        e.setEmailDelayMinutes(5);
        e.setIcpId(10);
        e.setLeadBatchId(20);
        e.setMailboxId(30);
        
        assertEquals(1, e.getId());
        assertEquals(2, e.getUserId());
        assertEquals("Name", e.getName());
        assertEquals("running", e.getStatus());
        assertEquals("file.csv", e.getCsvFilename());
        assertEquals("[]", e.getCsvColumns());
        assertEquals("Subj", e.getEmailSubject());
        assertEquals("Body", e.getEmailBody());
        assertNotNull(e.getStartAt());
        assertNotNull(e.getCreatedAt());
        assertEquals(5, e.getEmailDelayMinutes());
        assertEquals(10, e.getIcpId());
        assertEquals(20, e.getLeadBatchId());
        assertEquals(30, e.getMailboxId());
    }

    @Test
    void testLeadEntity() {
        LeadEntity e = new LeadEntity();
        e.setId(1);
        e.setUserId(2);
        e.setBatchId(3);
        e.setFirstName("First");
        e.setLastName("Last");
        e.setEmail("test@test.com");
        e.setCompanyName("Comp");
        e.setJobTitle("CEO");
        e.setLinkedinUrl("url");
        e.setCompanyWebsite("site");
        e.setCompanyLinkedin("clink");
        e.setCountry("Country");
        e.setCustomNotes("notes");
        e.setCsvData("{}");
        e.setCreatedAt(LocalDateTime.now());
        
        assertEquals(1, e.getId());
        assertEquals(2, e.getUserId());
        assertEquals(3, e.getBatchId());
        assertEquals("First", e.getFirstName());
        assertEquals("Last", e.getLastName());
        assertEquals("test@test.com", e.getEmail());
        assertEquals("Comp", e.getCompanyName());
        assertEquals("CEO", e.getJobTitle());
        assertEquals("url", e.getLinkedinUrl());
        assertEquals("site", e.getCompanyWebsite());
        assertEquals("clink", e.getCompanyLinkedin());
        assertEquals("Country", e.getCountry());
        assertEquals("notes", e.getCustomNotes());
        assertEquals("{}", e.getCsvData());
        assertNotNull(e.getCreatedAt());
    }
    
    @Test
    void testMailboxEntity() {
        MailboxEntity e = new MailboxEntity();
        e.setId(1);
        e.setUserId(2);
        e.setEmailAddress("test@gmail.com");
        e.setProvider("gmail");
        e.setDisplayName("Display");
        e.setIsVerified(true);
        e.setWarmupStatus("none");
        e.setSmtpHost("smtp");
        e.setSmtpPort(587);
        e.setImapHost("imap");
        e.setImapPort(993);
        e.setAccessToken("token");
        e.setRefreshToken("refresh");
        e.setTokenExpiresAt(LocalDateTime.now());
        e.setOauthProvider("google");
        e.setEncryptedPassword("pass");
        e.setCreatedAt(LocalDateTime.now());
        
        assertEquals(1, e.getId());
        assertEquals(2, e.getUserId());
        assertEquals("test@gmail.com", e.getEmailAddress());
        assertEquals("gmail", e.getProvider());
        assertEquals("Display", e.getDisplayName());
        assertTrue(e.getIsVerified());
        assertEquals("none", e.getWarmupStatus());
        assertEquals("smtp", e.getSmtpHost());
        assertEquals(587, e.getSmtpPort());
        assertEquals("imap", e.getImapHost());
        assertEquals(993, e.getImapPort());
        assertEquals("token", e.getAccessToken());
        assertEquals("refresh", e.getRefreshToken());
        assertNotNull(e.getTokenExpiresAt());
        assertEquals("google", e.getOauthProvider());
        assertEquals("pass", e.getEncryptedPassword());
        assertNotNull(e.getCreatedAt());
    }

    @Test
    void testCampaignMailboxEntity() {
        CampaignMailboxEntity e = new CampaignMailboxEntity();
        e.setId(1);
        e.setCampaignId(2);
        e.setMailboxId(3);
        e.setPriority(1);
        e.setRotationWeight(2);
        e.setCreatedAt(LocalDateTime.now());
        
        assertEquals(1, e.getId());
        assertEquals(2, e.getCampaignId());
        assertEquals(3, e.getMailboxId());
        assertEquals(1, e.getPriority());
        assertEquals(2, e.getRotationWeight());
        assertNotNull(e.getCreatedAt());
        
        CampaignMailboxEntity e2 = new CampaignMailboxEntity(2, 3, 1, 2);
        assertEquals(2, e2.getCampaignId());
        assertEquals(3, e2.getMailboxId());
    }
    
    @Test
    void testSentEmailEntity() {
        SentEmailEntity e = new SentEmailEntity();
        e.setId(1);
        e.setCampaignId(2);
        e.setLeadId(3);
        e.setMailboxId(4);
        e.setCampaignStepId(5);
        e.setToEmail("to@test.com");
        e.setStatus("sent");
        e.setProviderMessageId("msgId");
        e.setSubject("Subj");
        e.setBody("Body");
        e.setSentAt(LocalDateTime.now());
        e.setCreatedAt(LocalDateTime.now());
        
        assertEquals(1, e.getId());
        assertEquals(2, e.getCampaignId());
        assertEquals(3, e.getLeadId());
        assertEquals(4, e.getMailboxId());
        assertEquals(5, e.getCampaignStepId());
        assertEquals("to@test.com", e.getToEmail());
        assertEquals("sent", e.getStatus());
        assertEquals("msgId", e.getProviderMessageId());
        assertEquals("Subj", e.getSubject());
        assertEquals("Body", e.getBody());
        assertNotNull(e.getSentAt());
        assertNotNull(e.getCreatedAt());
    }
    
    @Test
    void testCampaignLeadEntity() {
        CampaignLeadEntity e = new CampaignLeadEntity();
        e.setId(1);
        e.setCampaignId(2);
        e.setLeadId(3);
        e.setStatus("active");
        e.setCreatedAt(LocalDateTime.now());
        
        assertEquals(1, e.getId());
        assertEquals(2, e.getCampaignId());
        assertEquals(3, e.getLeadId());
        assertEquals("active", e.getStatus());
        assertNotNull(e.getCreatedAt());
    }
}
