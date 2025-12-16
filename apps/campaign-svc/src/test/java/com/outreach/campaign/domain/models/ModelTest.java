package com.outreach.campaign.domain.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

class ModelTest {

    @Test
    void testCampaign() {
        Campaign c = new Campaign(
            "1", "Name", "Desc", Campaign.CampaignStatus.DRAFT, 
            LocalDateTime.now(), LocalDateTime.now(), 
            Collections.emptyList(), 0, 0, 0, 0, 
            "file.csv", Collections.emptyList(), "Subj", "Body"
        );
        
        assertEquals("1", c.id());
        assertEquals("Name", c.name());
        assertEquals(Campaign.CampaignStatus.DRAFT, c.status());
        assertEquals("file.csv", c.csvFilename());
        assertEquals("Subj", c.emailSubject());
    }
    
    @Test
    void testLead() {
        Lead l = new Lead(
            "1", 0.9, "Comp", "First", "Last", "Pos", "test@test.com", 
            "domain.com", "Tech", 100, "US", "Java", "Key", "Notes", "Hook"
        );
        
        assertEquals("1", l.id());
        assertEquals("First", l.firstName());
        assertEquals("test@test.com", l.email());
        assertEquals("Comp", l.company());
        assertEquals(0.9, l.matchScore());
    }
}
