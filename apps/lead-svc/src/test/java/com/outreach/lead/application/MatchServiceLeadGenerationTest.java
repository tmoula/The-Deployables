package com.outreach.lead.application;

import com.outreach.lead.api.DTO.LeadBatchResponse;
import com.outreach.lead.domain.ProspectCriteria;
import com.outreach.lead.domain.SellerProfile;
import com.outreach.lead.domain.entities.*;
import com.outreach.lead.infrastructure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceLeadGenerationTest {

    @Mock
    private ProspectService prospectService;
    @Mock
    private SenderCompanyRepository senderCompanyRepository;
    @Mock
    private ICPProfileRepository icpProfileRepository;
    @Mock
    private LeadBatchRepository leadBatchRepository;
    @Mock
    private RabbitMQClient rabbitMQClient;
    @Mock
    private UserContextService userContextService;

    @InjectMocks
    private MatchService matchService;

    private SellerProfile testSeller;
    private ProspectCriteria testCriteria;
    private Integer testUserId = 100;

    @BeforeEach
    void setUp() {
        testSeller = new SellerProfile(
            "Test Company",
            "Technology",
            100,
            2020,
            "North America",
            List.of("AI", "cloud"),
            "Enterprises",
            "Premium",
            List.of("Java", "AWS"),
            "Outbound",
            List.of("US", "EU")
        );

        testCriteria = new ProspectCriteria(
            null, null, "Technology", 50, 200,
            null, null, null, null, null,
            List.of("Java"), null,
            List.of("CEO", "CTO"), "C-level",
            "North America", "US", null, List.of("US"),
            null, null, List.of("enterprise"),
            null, null
        );
    }

    @Test
    void testStartLeadGeneration_Success() throws Exception {
        // Setup mocks
        SenderCompanyEntity savedSender = new SenderCompanyEntity();
        savedSender.setId(1);
        savedSender.setUserId(testUserId);
        savedSender.setName("Test Company");
        
        ICPProfileEntity savedICP = new ICPProfileEntity();
        savedICP.setId(1);
        savedICP.setUserId(testUserId);
        savedICP.setName("Technology - 50-200 employees");
        
        LeadBatchEntity savedBatch = new LeadBatchEntity();
        savedBatch.setId(1);
        savedBatch.setUserId(testUserId);
        savedBatch.setIcpId(1);
        savedBatch.setStatus("running");
        
        when(senderCompanyRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(senderCompanyRepository.save(any(SenderCompanyEntity.class))).thenReturn(savedSender);
        when(icpProfileRepository.findByUserIdAndTargetIndustryAndCompanySizeMinAndCompanySizeMax(
            eq(testUserId), eq("Technology"), eq(50), eq(200)
        )).thenReturn(Optional.empty());
        when(icpProfileRepository.save(any(ICPProfileEntity.class))).thenReturn(savedICP);
        when(leadBatchRepository.save(any(LeadBatchEntity.class))).thenReturn(savedBatch);
        doNothing().when(rabbitMQClient).generateMatchingCompanies(any(), anyInt(), anyInt());

        // Execute
        LeadBatchResponse response = matchService.startLeadGeneration(testSeller, testCriteria, 10, testUserId);

        // Verify
        assertNotNull(response);
        assertEquals(1, response.batchId());
        assertEquals("running", response.status());
        assertEquals("Lead generation started", response.message());

        // Verify interactions
        verify(senderCompanyRepository, times(1)).save(any(SenderCompanyEntity.class));
        verify(icpProfileRepository, times(1)).save(any(ICPProfileEntity.class));
        verify(leadBatchRepository, times(1)).save(any(LeadBatchEntity.class));
        verify(rabbitMQClient, times(1)).generateMatchingCompanies(eq(testCriteria), eq(5), eq(1));
    }

    @Test
    void testStartLeadGeneration_UpdatesExistingSenderCompany() throws Exception {
        // Setup existing sender company
        SenderCompanyEntity existingSender = new SenderCompanyEntity();
        existingSender.setId(1);
        existingSender.setUserId(testUserId);
        existingSender.setName("Old Company Name");
        
        SenderCompanyEntity updatedSender = new SenderCompanyEntity();
        updatedSender.setId(1);
        updatedSender.setUserId(testUserId);
        updatedSender.setName("Test Company");
        
        ICPProfileEntity savedICP = new ICPProfileEntity();
        savedICP.setId(1);
        
        LeadBatchEntity savedBatch = new LeadBatchEntity();
        savedBatch.setId(1);
        savedBatch.setUserId(testUserId);
        savedBatch.setIcpId(1);
        
        when(senderCompanyRepository.findByUserId(testUserId)).thenReturn(Optional.of(existingSender));
        when(senderCompanyRepository.save(any(SenderCompanyEntity.class))).thenReturn(updatedSender);
        when(icpProfileRepository.findByUserIdAndTargetIndustryAndCompanySizeMinAndCompanySizeMax(
            anyInt(), any(), any(), any()
        )).thenReturn(Optional.empty());
        when(icpProfileRepository.save(any(ICPProfileEntity.class))).thenReturn(savedICP);
        when(leadBatchRepository.save(any(LeadBatchEntity.class))).thenReturn(savedBatch);
        doNothing().when(rabbitMQClient).generateMatchingCompanies(any(), anyInt(), anyInt());

        // Execute
        LeadBatchResponse response = matchService.startLeadGeneration(testSeller, testCriteria, 10, testUserId);

        // Verify sender company was updated
        ArgumentCaptor<SenderCompanyEntity> captor = ArgumentCaptor.forClass(SenderCompanyEntity.class);
        verify(senderCompanyRepository).save(captor.capture());
        assertEquals("Test Company", captor.getValue().getName());
        assertEquals("Technology", captor.getValue().getIndustry());
    }

    @Test
    void testStartLeadGeneration_UpdatesExistingICPProfile() throws Exception {
        // Setup existing ICP profile
        ICPProfileEntity existingICP = new ICPProfileEntity();
        existingICP.setId(1);
        existingICP.setUserId(testUserId);
        existingICP.setTargetIndustry("Old Industry");
        
        SenderCompanyEntity savedSender = new SenderCompanyEntity();
        savedSender.setId(1);
        
        LeadBatchEntity savedBatch = new LeadBatchEntity();
        savedBatch.setId(1);
        savedBatch.setUserId(testUserId);
        savedBatch.setIcpId(1);
        
        when(senderCompanyRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(senderCompanyRepository.save(any(SenderCompanyEntity.class))).thenReturn(savedSender);
        when(icpProfileRepository.findByUserIdAndTargetIndustryAndCompanySizeMinAndCompanySizeMax(
            eq(testUserId), eq("Technology"), eq(50), eq(200)
        )).thenReturn(Optional.of(existingICP));
        when(icpProfileRepository.save(any(ICPProfileEntity.class))).thenReturn(existingICP);
        when(leadBatchRepository.save(any(LeadBatchEntity.class))).thenReturn(savedBatch);
        doNothing().when(rabbitMQClient).generateMatchingCompanies(any(), anyInt(), anyInt());

        // Execute
        LeadBatchResponse response = matchService.startLeadGeneration(testSeller, testCriteria, 10, testUserId);

        // Verify ICP was updated
        ArgumentCaptor<ICPProfileEntity> captor = ArgumentCaptor.forClass(ICPProfileEntity.class);
        verify(icpProfileRepository).save(captor.capture());
        assertEquals("Technology", captor.getValue().getTargetIndustry());
        assertEquals(50, captor.getValue().getCompanySizeMin());
        assertEquals(200, captor.getValue().getCompanySizeMax());
    }

    @Test
    void testStartLeadGeneration_LimitsLeadCountTo5() throws Exception {
        SenderCompanyEntity savedSender = new SenderCompanyEntity();
        savedSender.setId(1);
        
        ICPProfileEntity savedICP = new ICPProfileEntity();
        savedICP.setId(1);
        
        LeadBatchEntity savedBatch = new LeadBatchEntity();
        savedBatch.setId(1);
        savedBatch.setUserId(testUserId);
        savedBatch.setIcpId(1);
        
        when(senderCompanyRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(senderCompanyRepository.save(any(SenderCompanyEntity.class))).thenReturn(savedSender);
        when(icpProfileRepository.findByUserIdAndTargetIndustryAndCompanySizeMinAndCompanySizeMax(
            anyInt(), any(), any(), any()
        )).thenReturn(Optional.empty());
        when(icpProfileRepository.save(any(ICPProfileEntity.class))).thenReturn(savedICP);
        when(leadBatchRepository.save(any(LeadBatchEntity.class))).thenReturn(savedBatch);
        doNothing().when(rabbitMQClient).generateMatchingCompanies(any(), anyInt(), anyInt());

        // Execute with limit of 100
        matchService.startLeadGeneration(testSeller, testCriteria, 100, testUserId);

        // Verify limit was capped at 5
        ArgumentCaptor<LeadBatchEntity> batchCaptor = ArgumentCaptor.forClass(LeadBatchEntity.class);
        verify(leadBatchRepository).save(batchCaptor.capture());
        assertEquals(5, batchCaptor.getValue().getRequestedLeadCount());
        
        verify(rabbitMQClient).generateMatchingCompanies(any(), eq(5), anyInt());
    }

    @Test
    void testStartLeadGeneration_RabbitMQFailure() throws Exception {
        SenderCompanyEntity savedSender = new SenderCompanyEntity();
        savedSender.setId(1);
        
        ICPProfileEntity savedICP = new ICPProfileEntity();
        savedICP.setId(1);
        
        LeadBatchEntity savedBatch = new LeadBatchEntity();
        savedBatch.setId(1);
        savedBatch.setUserId(testUserId);
        savedBatch.setIcpId(1);
        
        when(senderCompanyRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(senderCompanyRepository.save(any(SenderCompanyEntity.class))).thenReturn(savedSender);
        when(icpProfileRepository.findByUserIdAndTargetIndustryAndCompanySizeMinAndCompanySizeMax(
            anyInt(), any(), any(), any()
        )).thenReturn(Optional.empty());
        when(icpProfileRepository.save(any(ICPProfileEntity.class))).thenReturn(savedICP);
        when(leadBatchRepository.save(any(LeadBatchEntity.class))).thenReturn(savedBatch);
        doThrow(new RuntimeException("RabbitMQ connection failed"))
            .when(rabbitMQClient).generateMatchingCompanies(any(), anyInt(), anyInt());

        // Execute and expect exception
        assertThrows(RuntimeException.class, () -> {
            matchService.startLeadGeneration(testSeller, testCriteria, 10, testUserId);
        });

        // Verify batch status was updated to failed
        verify(leadBatchRepository, times(2)).save(any(LeadBatchEntity.class));
    }

    @Test
    void testStartLeadGeneration_WithTargetRoles() throws Exception {
        ProspectCriteria criteriaWithRoles = new ProspectCriteria(
            null, null, "Technology", 50, 200,
            null, null, null, null, null,
            List.of("Java"), null,
            List.of("CEO", "CTO", "VP Engineering"), "C-level",
            null, null, null, List.of("US"),
            null, null, null, null, null
        );
        
        SenderCompanyEntity savedSender = new SenderCompanyEntity();
        savedSender.setId(1);
        
        ICPProfileEntity savedICP = new ICPProfileEntity();
        savedICP.setId(1);
        
        LeadBatchEntity savedBatch = new LeadBatchEntity();
        savedBatch.setId(1);
        savedBatch.setUserId(testUserId);
        savedBatch.setIcpId(1);
        
        when(senderCompanyRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(senderCompanyRepository.save(any(SenderCompanyEntity.class))).thenReturn(savedSender);
        when(icpProfileRepository.findByUserIdAndTargetIndustryAndCompanySizeMinAndCompanySizeMax(
            anyInt(), any(), any(), any()
        )).thenReturn(Optional.empty());
        when(icpProfileRepository.save(any(ICPProfileEntity.class))).thenReturn(savedICP);
        when(leadBatchRepository.save(any(LeadBatchEntity.class))).thenReturn(savedBatch);
        doNothing().when(rabbitMQClient).generateMatchingCompanies(any(), anyInt(), anyInt());

        // Execute
        matchService.startLeadGeneration(testSeller, criteriaWithRoles, 10, testUserId);

        // Verify target titles were set
        ArgumentCaptor<ICPProfileEntity> captor = ArgumentCaptor.forClass(ICPProfileEntity.class);
        verify(icpProfileRepository).save(captor.capture());
        assertEquals("CEO, CTO, VP Engineering", captor.getValue().getTargetTitles());
    }

    @Test
    void testStartLeadGeneration_WithRegions() throws Exception {
        ProspectCriteria criteriaWithRegions = new ProspectCriteria(
            null, null, "Technology", 50, 200,
            null, null, null, null, null,
            null, null, null, null,
            null, null, null, List.of("US", "EU", "APAC"),
            null, null, null, null, null
        );
        
        SenderCompanyEntity savedSender = new SenderCompanyEntity();
        savedSender.setId(1);
        
        ICPProfileEntity savedICP = new ICPProfileEntity();
        savedICP.setId(1);
        
        LeadBatchEntity savedBatch = new LeadBatchEntity();
        savedBatch.setId(1);
        savedBatch.setUserId(testUserId);
        savedBatch.setIcpId(1);
        
        when(senderCompanyRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(senderCompanyRepository.save(any(SenderCompanyEntity.class))).thenReturn(savedSender);
        when(icpProfileRepository.findByUserIdAndTargetIndustryAndCompanySizeMinAndCompanySizeMax(
            anyInt(), any(), any(), any()
        )).thenReturn(Optional.empty());
        when(icpProfileRepository.save(any(ICPProfileEntity.class))).thenReturn(savedICP);
        when(leadBatchRepository.save(any(LeadBatchEntity.class))).thenReturn(savedBatch);
        doNothing().when(rabbitMQClient).generateMatchingCompanies(any(), anyInt(), anyInt());

        // Execute
        matchService.startLeadGeneration(testSeller, criteriaWithRegions, 10, testUserId);

        // Verify geo region was set
        ArgumentCaptor<ICPProfileEntity> captor = ArgumentCaptor.forClass(ICPProfileEntity.class);
        verify(icpProfileRepository).save(captor.capture());
        assertEquals("US, EU, APAC", captor.getValue().getGeoRegion());
    }

    @Test
    void testStartLeadGeneration_WithHeadquartersRegionOnly() throws Exception {
        ProspectCriteria criteriaWithHQ = new ProspectCriteria(
            null, null, "Technology", 50, 200,
            null, null, null, null, null,
            null, null, null, null,
            "North America", null, null, null,
            null, null, null, null, null
        );
        
        SenderCompanyEntity savedSender = new SenderCompanyEntity();
        savedSender.setId(1);
        
        ICPProfileEntity savedICP = new ICPProfileEntity();
        savedICP.setId(1);
        
        LeadBatchEntity savedBatch = new LeadBatchEntity();
        savedBatch.setId(1);
        savedBatch.setUserId(testUserId);
        savedBatch.setIcpId(1);
        
        when(senderCompanyRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(senderCompanyRepository.save(any(SenderCompanyEntity.class))).thenReturn(savedSender);
        when(icpProfileRepository.findByUserIdAndTargetIndustryAndCompanySizeMinAndCompanySizeMax(
            anyInt(), any(), any(), any()
        )).thenReturn(Optional.empty());
        when(icpProfileRepository.save(any(ICPProfileEntity.class))).thenReturn(savedICP);
        when(leadBatchRepository.save(any(LeadBatchEntity.class))).thenReturn(savedBatch);
        doNothing().when(rabbitMQClient).generateMatchingCompanies(any(), anyInt(), anyInt());

        // Execute
        matchService.startLeadGeneration(testSeller, criteriaWithHQ, 10, testUserId);

        // Verify geo region was set from headquartersRegion
        ArgumentCaptor<ICPProfileEntity> captor = ArgumentCaptor.forClass(ICPProfileEntity.class);
        verify(icpProfileRepository).save(captor.capture());
        assertEquals("North America", captor.getValue().getGeoRegion());
    }

    @Test
    void testStartLeadGeneration_WithSellerHeadquartersRegion() throws Exception {
        SenderCompanyEntity savedSender = new SenderCompanyEntity();
        savedSender.setId(1);
        
        ICPProfileEntity savedICP = new ICPProfileEntity();
        savedICP.setId(1);
        
        LeadBatchEntity savedBatch = new LeadBatchEntity();
        savedBatch.setId(1);
        savedBatch.setUserId(testUserId);
        savedBatch.setIcpId(1);
        
        when(senderCompanyRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(senderCompanyRepository.save(any(SenderCompanyEntity.class))).thenReturn(savedSender);
        when(icpProfileRepository.findByUserIdAndTargetIndustryAndCompanySizeMinAndCompanySizeMax(
            anyInt(), any(), any(), any()
        )).thenReturn(Optional.empty());
        when(icpProfileRepository.save(any(ICPProfileEntity.class))).thenReturn(savedICP);
        when(leadBatchRepository.save(any(LeadBatchEntity.class))).thenReturn(savedBatch);
        doNothing().when(rabbitMQClient).generateMatchingCompanies(any(), anyInt(), anyInt());

        // Execute
        matchService.startLeadGeneration(testSeller, testCriteria, 10, testUserId);

        // Verify sender company description includes headquarters
        ArgumentCaptor<SenderCompanyEntity> captor = ArgumentCaptor.forClass(SenderCompanyEntity.class);
        verify(senderCompanyRepository).save(captor.capture());
        assertTrue(captor.getValue().getDescription().contains("North America"));
    }
}
