package com.outreach.campaign.application.csv;

import com.outreach.campaign.domain.models.Lead;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CsvParserServiceTest {

    private CsvParserService csvParserService;

    @BeforeEach
    void setUp() {
        csvParserService = new CsvParserService();
    }

    @Test
    void testParseCsvWithValidData() {
        // Given
        String csvContent = "First Name,Last Name,Email,Company\n" +
                           "John,Doe,john@example.com,Acme Corp\n" +
                           "Jane,Smith,jane@test.com,Tech Inc";
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CsvParseResult result = csvParserService.parseCsv(file);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getLeads().size());
        assertEquals(4, result.getColumns().size());
        assertTrue(result.getColumns().contains("First Name"));
        assertTrue(result.getColumns().contains("Email"));
        
        Lead firstLead = result.getLeads().get(0);
        assertEquals("John", firstLead.firstName());
        assertEquals("Doe", firstLead.lastName());
        assertEquals("john@example.com", firstLead.email());
        assertEquals("Acme Corp", firstLead.company());
    }

    @Test
    void testParseCsvWithHeaders() {
        // Given
        String csvContent = "Name,Email,Position,Company Name\n" +
                           "Alice,alice@company.com,CEO,BigCorp";
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CsvParseResult result = csvParserService.parseCsv(file);

        // Then
        assertEquals(1, result.getLeads().size());
        assertEquals(4, result.getColumns().size());
        assertEquals("alice@company.com", result.getLeads().get(0).email());
    }

    @Test
    void testParseCsvWithQuotedFields() {
        // Given
        String csvContent = "First Name,Last Name,Email,Company\n" +
                           "\"John, Jr.\",\"O'Brien\",john@example.com,\"Acme, Corp\"";
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CsvParseResult result = csvParserService.parseCsv(file);

        // Then
        assertEquals(1, result.getLeads().size());
        Lead lead = result.getLeads().get(0);
        assertEquals("John, Jr.", lead.firstName());
        assertEquals("O'Brien", lead.lastName());
        assertEquals("Acme, Corp", lead.company());
    }

    @Test
    void testParseCsvWithEmptyFile() {
        // Given
        String csvContent = "";
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // When/Then
        assertThrows(RuntimeException.class, () -> csvParserService.parseCsv(file));
    }

    @Test
    void testParseCsvWithOnlyHeaders() {
        // Given
        String csvContent = "First Name,Last Name,Email,Company";
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CsvParseResult result = csvParserService.parseCsv(file);

        // Then
        assertEquals(0, result.getLeads().size());
        assertEquals(4, result.getColumns().size());
    }

    @Test
    void testParseCsvWithInvalidEmail() {
        // Given
        String csvContent = "First Name,Email\n" +
                           "John,invalid-email\n" +
                           "Jane,jane@valid.com";
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CsvParseResult result = csvParserService.parseCsv(file);

        // Then
        assertEquals(1, result.getLeads().size()); // Only valid email row
        assertEquals("jane@valid.com", result.getLeads().get(0).email());
    }

    @Test
    void testParseCsvWithMissingEmail() {
        // Given
        String csvContent = "First Name,Last Name,Company\n" +
                           "John,Doe,Acme";
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CsvParseResult result = csvParserService.parseCsv(file);

        // Then
        assertEquals(0, result.getLeads().size()); // No email column = no leads
    }

    @Test
    void testParseCsvWithEmptyRows() {
        // Given
        String csvContent = "First Name,Email\n" +
                           "John,john@test.com\n" +
                           "\n" +
                           "Jane,jane@test.com";
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CsvParseResult result = csvParserService.parseCsv(file);

        // Then
        assertEquals(2, result.getLeads().size());
    }

    @Test
    void testParseCsvWithMatchScore() {
        // Given
        String csvContent = "Match Score,First Name,Email\n" +
                           "0.95,John,john@test.com\n" +
                           "invalid,Jane,jane@test.com";
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CsvParseResult result = csvParserService.parseCsv(file);

        // Then
        assertEquals(2, result.getLeads().size());
        assertEquals(0.95, result.getLeads().get(0).matchScore());
        assertNull(result.getLeads().get(1).matchScore()); // Invalid score
    }

    @Test
    void testParseCsvWithCompanySize() {
        // Given
        String csvContent = "Email,Company Size\n" +
                           "john@test.com,100\n" +
                           "jane@test.com,invalid";
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CsvParseResult result = csvParserService.parseCsv(file);

        // Then
        assertEquals(2, result.getLeads().size());
        assertEquals(100, result.getLeads().get(0).companySize());
        assertNull(result.getLeads().get(1).companySize());
    }

    @Test
    void testParseCsvWithFlexibleColumnNames() {
        // Given - Test various column name formats
        String csvContent = "first_name,Last_Name,E-mail,Company_Name\n" +
                           "John,Doe,john@test.com,Acme";
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CsvParseResult result = csvParserService.parseCsv(file);

        // Then
        assertEquals(1, result.getLeads().size());
        Lead lead = result.getLeads().get(0);
        assertEquals("John", lead.firstName());
        assertEquals("Doe", lead.lastName());
        assertEquals("john@test.com", lead.email());
        assertEquals("Acme", lead.company());
    }

    @Test
    void testParseCsvWithRawRowData() {
        // Given
        String csvContent = "First Name,Email,Custom Field\n" +
                           "John,john@test.com,CustomValue";
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CsvParseResult result = csvParserService.parseCsv(file);

        // Then
        assertEquals(1, result.getRawRowData().size());
        Map<String, String> rowData = result.getRawRowData().get(0);
        assertEquals("John", rowData.get("First Name"));
        assertEquals("john@test.com", rowData.get("Email"));
        assertEquals("CustomValue", rowData.get("Custom Field"));
    }

    @Test
    void testParseCsvWithSpecialCharacters() {
        // Given
        String csvContent = "First Name,Email,Company\n" +
                           "José,jose@test.com,Café & Co";
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CsvParseResult result = csvParserService.parseCsv(file);

        // Then
        assertEquals(1, result.getLeads().size());
        assertEquals("José", result.getLeads().get(0).firstName());
        assertEquals("Café & Co", result.getLeads().get(0).company());
    }

    @Test
    void testParseCsvWithEscapedQuotes() {
        // Given
        String csvContent = "First Name,Email,Notes\n" +
                           "John,john@test.com,\"He said \"\"Hello\"\"\"";
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CsvParseResult result = csvParserService.parseCsv(file);

        // Then
        assertEquals(1, result.getLeads().size());
        assertTrue(result.getRawRowData().get(0).get("Notes").contains("Hello"));
    }

    @Test
    void testParseCsvWithMultipleEmailFormats() {
        // Given - Test different email column names
        String csvContent = "Name,email_address,Company\n" +
                           "John,john@test.com,Acme";
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CsvParseResult result = csvParserService.parseCsv(file);

        // Then
        assertEquals(1, result.getLeads().size());
        assertEquals("john@test.com", result.getLeads().get(0).email());
    }

    @Test
    void testParseCsvWithAllStandardFields() {
        // Given
        String csvContent = "Match Score,Company,First Name,Last Name,Position,Email,Domain,Industry,Company Size,Regions,Tech Stack,Keywords,Notes,Personalization Hook\n" +
                           "0.9,Acme,John,Doe,CEO,john@acme.com,acme.com,Tech,100,US,Java,AI,Great lead,Personalized message";
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        // When
        CsvParseResult result = csvParserService.parseCsv(file);

        // Then
        assertEquals(1, result.getLeads().size());
        Lead lead = result.getLeads().get(0);
        assertEquals(0.9, lead.matchScore());
        assertEquals("Acme", lead.company());
        assertEquals("John", lead.firstName());
        assertEquals("Doe", lead.lastName());
        assertEquals("CEO", lead.position());
        assertEquals("john@acme.com", lead.email());
        assertEquals("acme.com", lead.domain());
        assertEquals("Tech", lead.industry());
        assertEquals(100, lead.companySize());
        assertEquals("US", lead.regions());
        assertEquals("Java", lead.techStack());
        assertEquals("AI", lead.keywords());
        assertEquals("Great lead", lead.notes());
        assertEquals("Personalized message", lead.personalizationHook());
    }
}
