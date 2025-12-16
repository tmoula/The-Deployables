package com.outreach.campaign.api;

import com.outreach.campaign.application.campaign.CampaignService;
import com.outreach.campaign.application.csv.CsvParseResult;
import com.outreach.campaign.application.csv.CsvParserService;
import com.outreach.campaign.application.csv.LeadImportService;
import com.outreach.campaign.application.lead.CampaignLeadService;
import com.outreach.campaign.application.rendering.EmailRenderService;
import com.outreach.campaign.application.common.UserContextService;
import com.outreach.campaign.domain.models.Campaign;
import com.outreach.campaign.domain.entities.CampaignEntity;
import com.outreach.campaign.domain.entities.SentEmailEntity;
import com.outreach.campaign.infrastructure.CampaignRepository;
import com.outreach.campaign.infrastructure.LeadRepository;
import com.outreach.campaign.infrastructure.SentEmailRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(CampaignController.class)
class CampaignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private CampaignService campaignService;
    @MockBean private CsvParserService csvParserService;
    @MockBean private LeadImportService leadImportService;
    @MockBean private UserContextService userContextService;
    @MockBean private LeadRepository leadRepository;
    @MockBean private CampaignLeadService campaignLeadService;
    @MockBean private EmailRenderService emailRenderService;
    @MockBean private SentEmailRepository sentEmailRepository;
    @MockBean private CampaignRepository campaignRepository;

    @Test
    void testUploadCsv() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "content".getBytes());
        CsvParseResult parseResult = new CsvParseResult(Collections.emptyList(), List.of("Col1"), Collections.emptyList());
        
        when(csvParserService.parseCsv(any())).thenReturn(parseResult);
        when(userContextService.getUserIdFromEmail(any())).thenReturn(1);
        when(campaignService.createCampaign(anyInt(), anyString(), any(), any(), anyString(), anyString()))
            .thenReturn(new Campaign("1", "Camp", "Desc", Campaign.CampaignStatus.DRAFT, LocalDateTime.now(), LocalDateTime.now(), Collections.emptyList(), 0, 0, 0, 0, "test.csv", List.of("Col1"), null, null));
        
        // Need to ensure validation passes (at least 1 lead usually required, but let's see)
        // Controller checks `leads.isEmpty()`
        // So I must return some leads in parseResult
        // But Lead constructor is complex?
        // Let's mock a lead with minimal data if possible, or just skip if logic allows empty (it fails if empty)
        
        // Let's try to mock return with leads. Since Lead is a record or simple class, I can't easily instantiate if I don't know it well?
        // Wait, I can see Lead usage in controller. `List<Lead> leads`.
        
        // Retrying with empty leads will cause 400 Bad Request "No valid leads found".
        // I will assert 400 for empty leads to test that path.
        
        mockMvc.perform(multipart("/api/v1/campaigns/upload-csv")
                .file(file)
                .param("campaignName", "Camp")
                .header("X-User-Email", "user@test.com"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllCampaigns() throws Exception {
        when(userContextService.getUserIdFromEmail(any())).thenReturn(1);
        when(campaignService.getAllCampaigns(1)).thenReturn(List.of(
            new Campaign("1", "C1", "", Campaign.CampaignStatus.DRAFT, LocalDateTime.now(), LocalDateTime.now(), Collections.emptyList(), 0, 0, 0, 0, "", Collections.emptyList(), null, null)
        ));

        mockMvc.perform(get("/api/v1/campaigns")
                .header("X-User-Email", "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"));
    }

    @Test
    void testPreviewEmail() throws Exception {
        Campaign camp = new Campaign("1", "C1", "", Campaign.CampaignStatus.DRAFT, LocalDateTime.now(), LocalDateTime.now(), Collections.emptyList(), 0, 0, 0, 0, "", List.of("Name"), null, null);
        when(campaignService.getCampaignById("1")).thenReturn(Optional.of(camp));
        
        // LeadEntity lead = ... (It's an entity, I can instantiate it)
        // But better to just ensure campaign exists and request body is present.
        // It calls campaignLeadService.getRandomLeadForCampaign
        
        when(campaignLeadService.getRandomLeadForCampaign(eq(1), anyLong())).thenReturn(null); 
        // If null lead, returns 400.
        
        mockMvc.perform(post("/api/v1/campaigns/1/preview-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\":\"Hi\", \"body\":\"Body\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateCampaignStatus() throws Exception {
        Campaign updated = new Campaign("1", "C1", "", Campaign.CampaignStatus.PAUSED, LocalDateTime.now(), LocalDateTime.now(), Collections.emptyList(), 0, 0, 0, 0, "", Collections.emptyList(), null, null);
        when(campaignService.updateCampaignStatus(eq("1"), eq(Campaign.CampaignStatus.PAUSED))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/campaigns/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PAUSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));
    }

    @Test
    void testDeleteCampaign() throws Exception {
        when(userContextService.getUserIdFromEmail(any())).thenReturn(1);
        
        mockMvc.perform(delete("/api/v1/campaigns/1")
                .header("X-User-Email", "user@test.com"))
                .andExpect(status().isNoContent());
                
        verify(campaignService).deleteCampaign("1", 1);
    }
    
    @Test
    void testScheduleCampaign() throws Exception {
        when(userContextService.getUserIdFromEmail(any())).thenReturn(1);
        
        mockMvc.perform(put("/api/v1/campaigns/1/schedule")
                .header("X-User-Email", "user@test.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"startDate\":\"2025-01-01\", \"selectedMailboxIds\":[1]}"))
                .andExpect(status().isOk());
                
        verify(campaignService).scheduleCampaign(eq(1), eq(1), any());
    }

    @Test
    void testGetCampaignStats() throws Exception {
        when(userContextService.getUserIdFromEmail(any())).thenReturn(1);
        
        CampaignEntity campaign = new CampaignEntity();
        campaign.setId(1);
        campaign.setUserId(1);
        campaign.setStatus("running");
        when(campaignRepository.findById(1)).thenReturn(Optional.of(campaign));
        
        SentEmailEntity sent1 = new SentEmailEntity(); sent1.setStatus("sent");
        SentEmailEntity sent2 = new SentEmailEntity(); sent2.setStatus("failed");
        when(sentEmailRepository.findByCampaignId(1)).thenReturn(List.of(sent1, sent2));
        when(campaignLeadService.getCampaignLeadCount(1)).thenReturn(10L);

        mockMvc.perform(get("/api/v1/campaigns/1/stats")
                .header("X-User-Email", "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentCount").value(1))
                .andExpect(jsonPath("$.failedCount").value(1))
                .andExpect(jsonPath("$.totalLeads").value(10));
    }
    @Test
    void testUploadCsv_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "content".getBytes());
        
        com.outreach.campaign.domain.models.Lead lead = new com.outreach.campaign.domain.models.Lead(
            "1", 0.9, "Comp", "First", "Last", "Pos", "test@test.com", 
            "domain.com", "Tech", 100, "US", "Java", "Key", "Notes", "Hook"
        );
        CsvParseResult parseResult = new CsvParseResult(List.of(lead), List.of("Col1"), List.of(Map.of("Col1", "Val1")));
        
        when(csvParserService.parseCsv(any())).thenReturn(parseResult);
        when(userContextService.getUserIdFromEmail(any())).thenReturn(1);
        when(campaignService.createCampaign(anyInt(), anyString(), any(), any(), anyString(), anyString()))
            .thenReturn(new Campaign("1", "Camp", "Desc", Campaign.CampaignStatus.DRAFT, LocalDateTime.now(), LocalDateTime.now(), List.of(lead), 1, 0, 0, 0, "test.csv", List.of("Col1"), null, null));
        when(leadImportService.importLeadsToDatabase(any(), any(), anyInt())).thenReturn(List.of(Map.of("id", 1)));
        
        mockMvc.perform(multipart("/api/v1/campaigns/upload-csv")
                .file(file)
                .param("campaignName", "Camp")
                .header("X-User-Email", "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leadsCount").value(1));
    }
    @Test
    void testSaveCampaignEmail() throws Exception {
        when(userContextService.getUserIdFromEmail(any())).thenReturn(1);
        doNothing().when(campaignService).saveCampaignEmail(eq(1), eq(1), eq("New Subject"), eq("New Body"));
        
        mockMvc.perform(put("/api/v1/campaigns/1/email")
                .contentType("application/json")
                .content("{\"emailSubject\":\"New Subject\", \"emailBody\":\"New Body\"}")
                .header("X-User-Email", "user@test.com"))
                .andExpect(status().isOk());
                
        verify(campaignService).saveCampaignEmail(1, 1, "New Subject", "New Body");
    }
}
