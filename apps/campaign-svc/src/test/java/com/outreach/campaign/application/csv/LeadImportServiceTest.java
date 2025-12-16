package com.outreach.campaign.application.csv;

import com.outreach.campaign.domain.entities.LeadEntity;
import com.outreach.campaign.domain.models.Lead;
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
class LeadImportServiceTest {

    @Mock
    private LeadRepository leadRepository;

    private LeadImportService leadImportService;

    @BeforeEach
    void setUp() {
        leadImportService = new LeadImportService(leadRepository);
    }

    @Test
    void testImportLeadsToDatabase() {
        // Given
        Lead lead = createTestLead("John", "Doe", "john@test.com", "Acme Corp");
        List<Lead> leads = Arrays.asList(lead);
        Map<String, String> rowData = new HashMap<>();
        rowData.put("First Name", "John");
        rowData.put("Last Name", "Doe");
        rowData.put("Email", "john@test.com");
        rowData.put("Company", "Acme Corp");
        List<Map<String, String>> rawRowData = Arrays.asList(rowData);
        Integer userId = 1;

        LeadEntity savedEntity = new LeadEntity();
        savedEntity.setId(100);
        savedEntity.setFirstName("John");
        when(leadRepository.save(any(LeadEntity.class))).thenReturn(savedEntity);

        // When
        List<Map<String, Object>> result = leadImportService.importLeadsToDatabase(leads, rawRowData, userId);

        // Then
        assertEquals(1, result.size());
        assertEquals(100, result.get(0).get("leadId"));
        assertEquals(1, result.get(0).get("userId"));
        assertEquals("John", result.get(0).get("firstName"));
        verify(leadRepository, times(1)).save(any(LeadEntity.class));
    }

    @Test
    void testImportLeadsWithCsvData() {
        // Given
        Lead lead = createTestLead("Jane", "Smith", "jane@test.com", "Tech Inc");
        List<Lead> leads = Arrays.asList(lead);
        Map<String, String> rowData = new HashMap<>();
        rowData.put("First Name", "Jane");
        rowData.put("Custom Field", "Custom Value");
        List<Map<String, String>> rawRowData = Arrays.asList(rowData);

        LeadEntity savedEntity = new LeadEntity();
        savedEntity.setId(200);
        when(leadRepository.save(any(LeadEntity.class))).thenReturn(savedEntity);

        // When
        List<Map<String, Object>> result = leadImportService.importLeadsToDatabase(leads, rawRowData, 1);

        // Then
        ArgumentCaptor<LeadEntity> captor = ArgumentCaptor.forClass(LeadEntity.class);
        verify(leadRepository).save(captor.capture());
        LeadEntity captured = captor.getValue();
        assertNotNull(captured.getCsvData());
        assertTrue(captured.getCsvData().contains("Custom Field"));
        assertTrue(captured.getCsvData().contains("Custom Value"));
    }

    @Test
    void testImportLeadsWithoutRawData() {
        // Given
        Lead lead = createTestLead("Bob", "Brown", "bob@test.com", "StartupXYZ");
        List<Lead> leads = Arrays.asList(lead);
        List<Map<String, String>> rawRowData = null; // No raw data

        LeadEntity savedEntity = new LeadEntity();
        savedEntity.setId(300);
        when(leadRepository.save(any(LeadEntity.class))).thenReturn(savedEntity);

        // When
        List<Map<String, Object>> result = leadImportService.importLeadsToDatabase(leads, rawRowData, 1);

        // Then
        assertEquals(1, result.size());
        verify(leadRepository, times(1)).save(any(LeadEntity.class));
    }

    @Test
    void testImportLeadsWithMatchScore() {
        // Given
        Lead lead = new Lead("1", 0.95, "Acme", "John", "Doe", "CEO", "john@test.com",
                            "acme.com", "Tech", 100, "US", "Java", "AI", "Notes", "Hook");
        List<Lead> leads = Arrays.asList(lead);

        LeadEntity savedEntity = new LeadEntity();
        savedEntity.setId(400);
        when(leadRepository.save(any(LeadEntity.class))).thenReturn(savedEntity);

        // When
        List<Map<String, Object>> result = leadImportService.importLeadsToDatabase(leads, null, 1);

        // Then
        ArgumentCaptor<LeadEntity> captor = ArgumentCaptor.forClass(LeadEntity.class);
        verify(leadRepository).save(captor.capture());
        LeadEntity captured = captor.getValue();
        assertTrue(captured.getCustomNotes().contains("Match: 0.95"));
    }

    @Test
    void testImportLeadsWithPersonalizationHook() {
        // Given
        Lead lead = new Lead("1", null, "Acme", "John", "Doe", "CEO", "john@test.com",
                            null, null, null, null, null, null, null, "Great personalization");
        List<Lead> leads = Arrays.asList(lead);

        LeadEntity savedEntity = new LeadEntity();
        savedEntity.setId(500);
        when(leadRepository.save(any(LeadEntity.class))).thenReturn(savedEntity);

        // When
        leadImportService.importLeadsToDatabase(leads, null, 1);

        // Then
        ArgumentCaptor<LeadEntity> captor = ArgumentCaptor.forClass(LeadEntity.class);
        verify(leadRepository).save(captor.capture());
        LeadEntity captured = captor.getValue();
        assertTrue(captured.getCustomNotes().contains("Hook: Great personalization"));
    }

    @Test
    void testImportLeadsWithIndustryAndSize() {
        // Given
        Lead lead = new Lead("1", null, "Acme", "John", "Doe", "CEO", "john@test.com",
                            null, "Technology", 500, "US", null, null, null, null);
        List<Lead> leads = Arrays.asList(lead);

        LeadEntity savedEntity = new LeadEntity();
        savedEntity.setId(600);
        when(leadRepository.save(any(LeadEntity.class))).thenReturn(savedEntity);

        // When
        leadImportService.importLeadsToDatabase(leads, null, 1);

        // Then
        ArgumentCaptor<LeadEntity> captor = ArgumentCaptor.forClass(LeadEntity.class);
        verify(leadRepository).save(captor.capture());
        LeadEntity captured = captor.getValue();
        assertTrue(captured.getCustomNotes().contains("Industry: Technology"));
        assertTrue(captured.getCustomNotes().contains("Size: 500"));
    }

    @Test
    void testImportLeadsWithTechStackAndKeywords() {
        // Given
        Lead lead = new Lead("1", null, "Acme", "John", "Doe", "CEO", "john@test.com",
                            null, null, null, null, "Python,AWS", "AI,ML", null, null);
        List<Lead> leads = Arrays.asList(lead);

        LeadEntity savedEntity = new LeadEntity();
        savedEntity.setId(700);
        when(leadRepository.save(any(LeadEntity.class))).thenReturn(savedEntity);

        // When
        leadImportService.importLeadsToDatabase(leads, null, 1);

        // Then
        ArgumentCaptor<LeadEntity> captor = ArgumentCaptor.forClass(LeadEntity.class);
        verify(leadRepository).save(captor.capture());
        LeadEntity captured = captor.getValue();
        assertTrue(captured.getCustomNotes().contains("Tech: Python,AWS"));
        assertTrue(captured.getCustomNotes().contains("Keywords: AI,ML"));
    }

    @Test
    void testImportMultipleLeads() {
        // Given
        Lead lead1 = createTestLead("John", "Doe", "john@test.com", "Acme");
        Lead lead2 = createTestLead("Jane", "Smith", "jane@test.com", "Tech Inc");
        List<Lead> leads = Arrays.asList(lead1, lead2);

        LeadEntity savedEntity1 = new LeadEntity();
        savedEntity1.setId(100);
        LeadEntity savedEntity2 = new LeadEntity();
        savedEntity2.setId(200);
        when(leadRepository.save(any(LeadEntity.class)))
            .thenReturn(savedEntity1)
            .thenReturn(savedEntity2);

        // When
        List<Map<String, Object>> result = leadImportService.importLeadsToDatabase(leads, null, 1);

        // Then
        assertEquals(2, result.size());
        verify(leadRepository, times(2)).save(any(LeadEntity.class));
    }

    @Test
    void testImportLeadsWithLongFields() {
        // Given
        String longName = "A".repeat(600); // Exceeds 500 char limit
        Lead lead = createTestLead(longName, "Doe", "john@test.com", "Acme");
        List<Lead> leads = Arrays.asList(lead);

        LeadEntity savedEntity = new LeadEntity();
        savedEntity.setId(800);
        when(leadRepository.save(any(LeadEntity.class))).thenReturn(savedEntity);

        // When
        leadImportService.importLeadsToDatabase(leads, null, 1);

        // Then
        ArgumentCaptor<LeadEntity> captor = ArgumentCaptor.forClass(LeadEntity.class);
        verify(leadRepository).save(captor.capture());
        LeadEntity captured = captor.getValue();
        // Should be truncated to 500 chars
        assertEquals(500, captured.getFirstName().length());
    }

    @Test
    void testImportLeadsWithNullFields() {
        // Given
        Lead lead = new Lead("1", null, null, null, null, null, "john@test.com",
                            null, null, null, null, null, null, null, null);
        List<Lead> leads = Arrays.asList(lead);

        LeadEntity savedEntity = new LeadEntity();
        savedEntity.setId(900);
        when(leadRepository.save(any(LeadEntity.class))).thenReturn(savedEntity);

        // When
        List<Map<String, Object>> result = leadImportService.importLeadsToDatabase(leads, null, 1);

        // Then
        assertEquals(1, result.size());
        verify(leadRepository, times(1)).save(any(LeadEntity.class));
    }

    @Test
    void testImportLeadsWithDatabaseError() {
        // Given
        Lead lead = createTestLead("John", "Doe", "john@test.com", "Acme");
        List<Lead> leads = Arrays.asList(lead);

        when(leadRepository.save(any(LeadEntity.class)))
            .thenThrow(new org.springframework.dao.DataAccessException("DB Error") {});

        // When/Then
        assertThrows(RuntimeException.class, () -> 
            leadImportService.importLeadsToDatabase(leads, null, 1));
    }

    @Test
    void testImportLeadsWithEmptyList() {
        // Given
        List<Lead> leads = new ArrayList<>();

        // When
        List<Map<String, Object>> result = leadImportService.importLeadsToDatabase(leads, null, 1);

        // Then
        assertEquals(0, result.size());
        verify(leadRepository, never()).save(any(LeadEntity.class));
    }

    @Test
    void testImportLeadsStoresUserId() {
        // Given
        Lead lead = createTestLead("John", "Doe", "john@test.com", "Acme");
        List<Lead> leads = Arrays.asList(lead);
        Integer userId = 42;

        LeadEntity savedEntity = new LeadEntity();
        savedEntity.setId(1000);
        when(leadRepository.save(any(LeadEntity.class))).thenReturn(savedEntity);

        // When
        leadImportService.importLeadsToDatabase(leads, null, userId);

        // Then
        ArgumentCaptor<LeadEntity> captor = ArgumentCaptor.forClass(LeadEntity.class);
        verify(leadRepository).save(captor.capture());
        LeadEntity captured = captor.getValue();
        assertEquals(userId, captured.getUserId());
    }

    // Helper method
    private Lead createTestLead(String firstName, String lastName, String email, String company) {
        return new Lead(
            UUID.randomUUID().toString(),
            null, // matchScore
            company,
            firstName,
            lastName,
            null, // position
            email,
            null, // domain
            null, // industry
            null, // companySize
            null, // regions
            null, // techStack
            null, // keywords
            null, // notes
            null  // personalizationHook
        );
    }
}
