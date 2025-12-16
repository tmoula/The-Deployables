package com.outreach.campaign.application.campaign;

import com.outreach.campaign.application.lead.CampaignLeadService;
import com.outreach.campaign.domain.models.Campaign;
import com.outreach.campaign.domain.entities.CampaignEntity;
import com.outreach.campaign.domain.entities.CampaignMailboxEntity;
import com.outreach.campaign.infrastructure.CampaignRepository;
import com.outreach.campaign.infrastructure.CampaignMailboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private CampaignLeadService campaignLeadService;
    @Mock
    private CampaignMailboxRepository campaignMailboxRepository;

    private CampaignService campaignService;

    @BeforeEach
    void setUp() {
        campaignService = new CampaignService(campaignRepository, campaignLeadService, campaignMailboxRepository);
    }

    @Test
    void testCreateCampaign_New() {
        // Given
        when(campaignRepository.findByUserIdAndName(1, "Camp1")).thenReturn(Optional.empty());
        when(campaignRepository.save(any(CampaignEntity.class))).thenAnswer(i -> {
            CampaignEntity e = i.getArgument(0);
            e.setId(100);
            e.setCreatedAt(LocalDateTime.now());
            return e;
        });

        // When
        Campaign result = campaignService.createCampaign(1, "Camp1", "Desc", Collections.emptyList(), "file.csv", "[\"col1\", \"col2\"]");

        // Then
        assertNotNull(result);
        assertEquals("100", result.id());
        assertEquals("Camp1", result.name());
        assertEquals("file.csv", result.csvFilename());
        assertEquals(2, result.csvColumns().size());
        verify(campaignRepository).save(any(CampaignEntity.class));
    }

    @Test
    void testCreateCampaign_Existing() {
        // Given
        CampaignEntity existing = new CampaignEntity();
        existing.setId(100);
        existing.setName("Camp1");
        existing.setStatus("draft");
        existing.setCreatedAt(LocalDateTime.now());
        existing.setCsvColumns("[]");

        when(campaignRepository.findByUserIdAndName(1, "Camp1")).thenReturn(Optional.of(existing));
        when(campaignRepository.save(any(CampaignEntity.class))).thenReturn(existing);
        when(campaignLeadService.getCampaignLeadCount(100)).thenReturn(5L);

        // When
        Campaign result = campaignService.createCampaign(1, "Camp1", "Desc", Collections.emptyList(), "new.csv", null);

        // Then
        assertEquals("100", result.id());
        assertEquals("new.csv", result.csvFilename()); // file name updated
        verify(campaignRepository).save(existing);
    }

    @Test
    void testGetAllCampaigns() {
        CampaignEntity e1 = new CampaignEntity();
        e1.setId(1);
        e1.setName("C1");
        e1.setStatus("draft");
        e1.setCreatedAt(LocalDateTime.now());
        e1.setUserId(1);
        e1.setCsvColumns("[]");

        when(campaignRepository.findByUserId(1)).thenReturn(List.of(e1));
        when(campaignLeadService.getCampaignLeadCount(1)).thenReturn(10L);

        List<Campaign> campaigns = campaignService.getAllCampaigns(1);
        assertEquals(1, campaigns.size());
        assertEquals("1", campaigns.get(0).id());
        assertEquals(10, campaigns.get(0).totalLeads());
    }

    @Test
    void testScheduleCampaign_Future() {
        // Given
        CampaignEntity e = new CampaignEntity();
        e.setId(1);
        e.setUserId(1);
        e.setStatus("draft");

        when(campaignRepository.findById(1)).thenReturn(Optional.of(e));

        // Future date
        String futureDate = LocalDateTime.now().plusDays(1).toString();
        Map<String, Object> settings = Map.of(
            "startDate", futureDate,
            "emailDelayMinutes", 10,
            "selectedMailboxIds", List.of(1, 2)
        );

        // When
        campaignService.scheduleCampaign(1, 1, settings);

        // Then
        verify(campaignMailboxRepository).deleteByCampaignId(1);
        verify(campaignMailboxRepository, times(2)).save(any(CampaignMailboxEntity.class));
        verify(campaignRepository).save(e);
        assertEquals("scheduled", e.getStatus());
        assertEquals(10, e.getEmailDelayMinutes());
        assertNotNull(e.getStartAt());
    }

    @Test
    void testGetCampaignById() {
        // Given
        CampaignEntity e = new CampaignEntity();
        e.setId(1);
        e.setName("C1");
        e.setStatus("draft");
        e.setCreatedAt(LocalDateTime.now());
        e.setUserId(1);
        e.setCsvColumns("[]");

        when(campaignRepository.findById(1)).thenReturn(Optional.of(e));
        when(campaignLeadService.getCampaignLeadCount(1)).thenReturn(5L);

        // When
        Optional<Campaign> result = campaignService.getCampaignById("1");

        // Then
        assertTrue(result.isPresent());
        assertEquals("1", result.get().id());
        assertEquals(5, result.get().totalLeads());
    }

    @Test
    void testDeleteCampaign() {
        // Given
        CampaignEntity e = new CampaignEntity();
        e.setId(1);
        e.setUserId(1);
        when(campaignRepository.findById(1)).thenReturn(Optional.of(e));

        // When
        campaignService.deleteCampaign("1", 1);

        // Then
        verify(campaignRepository).delete(e);
    }
    
    @Test
    void testDeleteCampaign_NotFound() {
        // Given
        when(campaignRepository.findById(1)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(RuntimeException.class, () -> campaignService.deleteCampaign("1", 1));
    }
    
    @Test
    void testDeleteCampaign_WrongUser() {
        // Given
        CampaignEntity e = new CampaignEntity();
        e.setId(1);
        e.setUserId(2); // Diff user
        when(campaignRepository.findById(1)).thenReturn(Optional.of(e));

        // When/Then
        assertThrows(RuntimeException.class, () -> campaignService.deleteCampaign("1", 1));
    }

    @Test
    void testUpdateCampaignStatus() {
        // Given
        CampaignEntity e = new CampaignEntity();
        e.setId(1);
        e.setUserId(1);
        e.setStatus("running");
        e.setCsvColumns("[]");
        
        when(campaignRepository.findById(1)).thenReturn(Optional.of(e));
        when(campaignRepository.save(any(CampaignEntity.class))).thenAnswer(i -> i.getArgument(0));

        // When
        Campaign result = campaignService.updateCampaignStatus("1", Campaign.CampaignStatus.PAUSED);

        // Then
        assertEquals("PAUSED", result.status().name());
        assertEquals("paused", e.getStatus());
    }

    @Test
    void testUpdateCampaignStatus_NotFound() {
        // Given
        when(campaignRepository.findById(1)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> campaignService.updateCampaignStatus("1", Campaign.CampaignStatus.PAUSED));
    }
    
    @Test
    void testScheduleCampaign_Immediate() {
        // Given
        CampaignEntity e = new CampaignEntity();
        e.setId(1);
        e.setUserId(1);
        e.setStatus("draft");

        when(campaignRepository.findById(1)).thenReturn(Optional.of(e));

        // Past startDate means immediate execution, status should be 'running'
        Map<String, Object> settings = new HashMap<>();
        settings.put("startDate", "2020-01-01T00:00:00Z"); // Past date = running
        settings.put("emailDelayMinutes", 5);
        settings.put("selectedMailboxIds", List.of(1));

        // When
        campaignService.scheduleCampaign(1, 1, settings);

        // Then
        assertEquals("running", e.getStatus());
        assertNotNull(e.getStartAt()); // Should be approx now
    }
}
