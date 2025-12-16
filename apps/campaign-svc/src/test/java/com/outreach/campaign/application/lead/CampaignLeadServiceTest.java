package com.outreach.campaign.application.lead;

import com.outreach.campaign.domain.entities.CampaignLeadEntity;
import com.outreach.campaign.domain.entities.LeadEntity;
import com.outreach.campaign.infrastructure.CampaignLeadRepository;
import com.outreach.campaign.infrastructure.LeadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignLeadServiceTest {

    @Mock
    private CampaignLeadRepository campaignLeadRepository;
    @Mock
    private LeadRepository leadRepository;

    private CampaignLeadService campaignLeadService;

    @BeforeEach
    void setUp() {
        campaignLeadService = new CampaignLeadService(campaignLeadRepository, leadRepository);
    }

    @Test
    void testLinkLeadsToCampaign() {
        // Given
        Integer campaignId = 1;
        List<Map<String, Object>> importedLeads = new ArrayList<>();
        Map<String, Object> leadInfo = new HashMap<>();
        leadInfo.put("leadId", 100);
        importedLeads.add(leadInfo);

        when(campaignLeadRepository.findByCampaignId(campaignId)).thenReturn(Collections.emptyList());

        // When
        campaignLeadService.linkLeadsToCampaign(campaignId, importedLeads);

        // Then
        ArgumentCaptor<CampaignLeadEntity> captor = ArgumentCaptor.forClass(CampaignLeadEntity.class);
        verify(campaignLeadRepository).save(captor.capture());
        CampaignLeadEntity saved = captor.getValue();
        assertEquals(campaignId, saved.getCampaignId());
        assertEquals(100, saved.getLeadId());
        assertEquals("queued", saved.getStatus());
    }

    @Test
    void testLinkLeadsToCampaignRemovesExisting() {
        // Given
        Integer campaignId = 1;
        CampaignLeadEntity existing = new CampaignLeadEntity();
        existing.setId(5);
        when(campaignLeadRepository.findByCampaignId(campaignId)).thenReturn(Arrays.asList(existing));

        List<Map<String, Object>> importedLeads = new ArrayList<>();
        Map<String, Object> leadInfo = new HashMap<>();
        leadInfo.put("leadId", 100);
        importedLeads.add(leadInfo);

        // When
        campaignLeadService.linkLeadsToCampaign(campaignId, importedLeads);

        // Then
        verify(campaignLeadRepository).deleteAll(anyList());
        verify(campaignLeadRepository).save(any(CampaignLeadEntity.class));
    }

    @Test
    void testLinkLeadsToCampaignIgnoresMissingLeadId() {
        // Given
        Integer campaignId = 1;
        List<Map<String, Object>> importedLeads = new ArrayList<>();
        Map<String, Object> leadInfo = new HashMap<>();
        // No leadId provided
        importedLeads.add(leadInfo);

        when(campaignLeadRepository.findByCampaignId(campaignId)).thenReturn(Collections.emptyList());

        // When
        campaignLeadService.linkLeadsToCampaign(campaignId, importedLeads);

        // Then
        verify(campaignLeadRepository, never()).save(any(CampaignLeadEntity.class));
    }
    
    @Test
    void testLinkLeadsToCampaignHandlesException() {
        // Given
        Integer campaignId = 1;
        List<Map<String, Object>> importedLeads = new ArrayList<>();
        Map<String, Object> leadInfo = new HashMap<>();
        leadInfo.put("leadId", 100);
        importedLeads.add(leadInfo);
        
        when(campaignLeadRepository.findByCampaignId(campaignId)).thenReturn(Collections.emptyList());
        doThrow(new RuntimeException("DB Error")).when(campaignLeadRepository).save(any());

        // When
        // Should catch exception and not propagate
        campaignLeadService.linkLeadsToCampaign(campaignId, importedLeads);

        // Then
        verify(campaignLeadRepository).save(any());
    }

    @Test
    void testGetCampaignLeads() {
        // Given
        Integer campaignId = 1;
        CampaignLeadEntity cl1 = new CampaignLeadEntity(); cl1.setLeadId(100);
        CampaignLeadEntity cl2 = new CampaignLeadEntity(); cl2.setLeadId(101);
        when(campaignLeadRepository.findByCampaignId(campaignId)).thenReturn(Arrays.asList(cl1, cl2));

        LeadEntity l1 = new LeadEntity(); l1.setId(100);
        LeadEntity l2 = new LeadEntity(); l2.setId(101);
        when(leadRepository.findById(100)).thenReturn(Optional.of(l1));
        when(leadRepository.findById(101)).thenReturn(Optional.of(l2));

        // When
        List<LeadEntity> result = campaignLeadService.getCampaignLeads(campaignId);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains(l1));
        assertTrue(result.contains(l2));
    }

    @Test
    void testGetCampaignLeadsWithMissingLead() {
        // Given
        Integer campaignId = 1;
        CampaignLeadEntity cl1 = new CampaignLeadEntity(); cl1.setLeadId(100);
        when(campaignLeadRepository.findByCampaignId(campaignId)).thenReturn(Arrays.asList(cl1));

        when(leadRepository.findById(100)).thenReturn(Optional.empty());

        // When
        List<LeadEntity> result = campaignLeadService.getCampaignLeads(campaignId);

        // Then
        assertEquals(0, result.size());
    }
    
    @Test
    void testGetCampaignLeadsException() {
        // Given
        when(campaignLeadRepository.findByCampaignId(anyInt())).thenThrow(new RuntimeException("DB Error"));

        // When/Then
        assertThrows(RuntimeException.class, () -> campaignLeadService.getCampaignLeads(1));
    }

    @Test
    void testGetCampaignLeadCount() {
        // Given
        Integer campaignId = 1;
        CampaignLeadEntity cl1 = new CampaignLeadEntity(); cl1.setLeadId(100);
        CampaignLeadEntity cl2 = new CampaignLeadEntity(); cl2.setLeadId(101);
        when(campaignLeadRepository.findByCampaignId(campaignId)).thenReturn(Arrays.asList(cl1, cl2));

        LeadEntity l1 = new LeadEntity(); l1.setId(100); l1.setEmail("valid@email.com");
        LeadEntity l2 = new LeadEntity(); l2.setId(101); l2.setEmail(""); // Invalid
        when(leadRepository.findById(100)).thenReturn(Optional.of(l1));
        when(leadRepository.findById(101)).thenReturn(Optional.of(l2));

        // When
        long count = campaignLeadService.getCampaignLeadCount(campaignId);

        // Then
        assertEquals(1, count); // Only valid email counted
    }
    
    @Test
    void testGetRandomLeadForCampaign() {
         // Given
        Integer campaignId = 1;
        CampaignLeadEntity cl1 = new CampaignLeadEntity(); cl1.setLeadId(100);
        when(campaignLeadRepository.findByCampaignId(campaignId)).thenReturn(Arrays.asList(cl1));
        
        LeadEntity l1 = new LeadEntity(); l1.setId(100);
        when(leadRepository.findById(100)).thenReturn(Optional.of(l1));

        // When
        LeadEntity result = campaignLeadService.getRandomLeadForCampaign(campaignId);

        // Then
        assertEquals(l1, result);
    }
    
    @Test
    void testGetRandomLeadForCampaignNoLeads() {
        // Given
        when(campaignLeadRepository.findByCampaignId(anyInt())).thenReturn(Collections.emptyList());

        // When
        LeadEntity result = campaignLeadService.getRandomLeadForCampaign(1);

        // Then
        assertNull(result);
    }
    
    @Test
    void testGetRandomLeadForCampaignWithCsvDataPreference() {
        // Given
        Integer campaignId = 1;
        CampaignLeadEntity cl1 = new CampaignLeadEntity(); cl1.setLeadId(100);
        CampaignLeadEntity cl2 = new CampaignLeadEntity(); cl2.setLeadId(101);
        when(campaignLeadRepository.findByCampaignId(campaignId)).thenReturn(Arrays.asList(cl1, cl2));
        
        LeadEntity l1 = new LeadEntity(); l1.setId(100); l1.setCsvData(null);
        LeadEntity l2 = new LeadEntity(); l2.setId(101); l2.setCsvData("{\"some\":\"data\"}");
        
        when(leadRepository.findById(100)).thenReturn(Optional.of(l1));
        when(leadRepository.findById(101)).thenReturn(Optional.of(l2));

        // When
        LeadEntity result = campaignLeadService.getRandomLeadForCampaign(campaignId);

        // Then
        // Should prefer l2 because it has CSV data
        assertEquals(l2, result);
    }

    @Test
    void testGetLeadWithCsvData() {
        // Given
        Integer campaignId = 1;
        CampaignLeadEntity cl1 = new CampaignLeadEntity(); cl1.setLeadId(100);
        when(campaignLeadRepository.findByCampaignId(campaignId)).thenReturn(Arrays.asList(cl1));
        
        LeadEntity l1 = new LeadEntity(); l1.setId(100); l1.setCsvData("{\"some\":\"data\"}");
        when(leadRepository.findById(100)).thenReturn(Optional.of(l1));

        // When
        LeadEntity result = campaignLeadService.getLeadWithCsvData(campaignId);

        // Then
        assertEquals(l1, result);
    }
    
    @Test
    void testGetLeadWithCsvDataNoneFound() {
        // Given
        Integer campaignId = 1;
        CampaignLeadEntity cl1 = new CampaignLeadEntity(); cl1.setLeadId(100);
        when(campaignLeadRepository.findByCampaignId(campaignId)).thenReturn(Arrays.asList(cl1));
        
        LeadEntity l1 = new LeadEntity(); l1.setId(100); l1.setCsvData(null);
        when(leadRepository.findById(100)).thenReturn(Optional.of(l1));

        // When
        LeadEntity result = campaignLeadService.getLeadWithCsvData(campaignId);

        // Then
        assertNull(result);
    }
}
