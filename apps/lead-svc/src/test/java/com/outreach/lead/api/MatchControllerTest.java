package com.outreach.lead.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.outreach.lead.application.MatchService;
import com.outreach.lead.domain.Prospect;
import com.outreach.lead.domain.ProspectCriteria;
import com.outreach.lead.domain.SellerProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatchController.class)
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchService matchService;

    @Autowired
    private ObjectMapper objectMapper;

    private SellerProfile testSeller;
    private Prospect testProspect;
    private ProspectCriteria testCriteria;

    @BeforeEach
    void setUp() {
        testSeller = new SellerProfile(
            "Test Company",           // companyName
            "Technology",             // industry
            100,                      // companySize
            2020,                     // foundedYear
            "North America",          // headquartersRegion
            List.of("AI", "cloud"),   // valuePropositionKeywords
            "Enterprises",            // targetCustomerSegment
            "Premium",                // priceTier
            List.of("Java", "AWS"),   // techStack
            "Outbound",               // salesModel
            List.of("US", "EU")       // targetRegions
        );

        testProspect = new Prospect(
            "1",                      // id
            "Test Prospect",          // company
            "John",                   // firstName
            "Doe",                    // lastName
            "CTO",                    // position
            "john.doe@test.com",      // email
            "test.com",               // domain
            "Technology",             // industry
            150,                      // size
            List.of("US"),            // regions
            List.of("Java", "AWS"),   // stack
            List.of("enterprise"),    // keywords
            null                      // personalizationHook
        );

        testCriteria = new ProspectCriteria(
            null,                     // companyName
            null,                     // domain
            "Technology",             // industry
            50,                       // minSize
            200,                      // maxSize
            null,                     // minFoundedYear
            null,                     // maxFoundedYear
            null,                     // fundingStage
            null,                     // annualRevenueRange
            null,                     // growthRate
            List.of("Java"),          // techUsed
            null,                     // techCategory
            null,                     // targetRoles
            null,                     // seniorityLevel
            null,                     // headquartersRegion
            null,                     // hqCountry
            null,                     // remoteFriendly
            List.of("US"),            // regions
            null,                     // hiringTrends
            null,                     // recentTechAdoption
            List.of("enterprise"),    // keywordMentions
            null,                     // requiredStack (legacy)
            null                      // desiredKeywords (legacy)
        );
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void testSetSeller() throws Exception {
        when(matchService.setSeller(any(SellerProfile.class))).thenReturn(testSeller);

        mockMvc.perform(put("/api/v1/seller")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testSeller)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.companyName").value("Test Company"))
            .andExpect(jsonPath("$.industry").value("Technology"))
            .andExpect(jsonPath("$.companySize").value(100));
    }

    @Test
    void testListProspects() throws Exception {
        List<Prospect> prospects = Arrays.asList(testProspect);
        when(matchService.listProspects()).thenReturn(prospects);

        mockMvc.perform(get("/api/v1/prospects"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].company").value("Test Prospect"))
            .andExpect(jsonPath("$[0].industry").value("Technology"));
    }

    @Test
    void testCreateProspect() throws Exception {
        when(matchService.createProspect(any(Prospect.class))).thenReturn(testProspect);

        mockMvc.perform(post("/api/v1/prospects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testProspect)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.company").value("Test Prospect"))
            .andExpect(jsonPath("$.domain").value("test.com"));
    }

    @Test
    void testMatchProspects() throws Exception {
        MatchService.ScoredProspect scoredProspect = new MatchService.ScoredProspect(testProspect, 75.5);
        List<MatchService.ScoredProspect> results = Arrays.asList(scoredProspect);

        when(matchService.match(any(ProspectCriteria.class), anyInt())).thenReturn(results);

        mockMvc.perform(post("/api/v1/match?limit=10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCriteria)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].score").value(75.5))
            .andExpect(jsonPath("$[0].prospect.company").value("Test Prospect"));
    }

    @Test
    void testMatchProspectsWithNullCriteria() throws Exception {
        MatchService.ScoredProspect scoredProspect = new MatchService.ScoredProspect(testProspect, 50.0);
        List<MatchService.ScoredProspect> results = Arrays.asList(scoredProspect);

        when(matchService.match(any(ProspectCriteria.class), anyInt())).thenReturn(results);

        mockMvc.perform(post("/api/v1/match")
                .contentType(MediaType.APPLICATION_JSON)
                .content("null"))
            .andExpect(status().isOk());
    }

    @Test
    void testMatchProspectsWithDefaultLimit() throws Exception {
        MatchService.ScoredProspect scoredProspect = new MatchService.ScoredProspect(testProspect, 60.0);
        List<MatchService.ScoredProspect> results = Arrays.asList(scoredProspect);

        when(matchService.match(any(ProspectCriteria.class), anyInt())).thenReturn(results);

        mockMvc.perform(post("/api/v1/match")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCriteria)))
            .andExpect(status().isOk());
    }
}

