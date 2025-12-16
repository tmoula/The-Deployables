package com.outreach.campaign.application.rendering;

import com.outreach.campaign.domain.entities.LeadEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmailRenderServiceTest {

    private EmailRenderService emailRenderService;

    @BeforeEach
    void setUp() {
        emailRenderService = new EmailRenderService();
    }

    @Test
    void testRenderEmailWithSimpleVariables() {
        // Given
        LeadEntity lead = createTestLead("John", "Doe", "Acme Corp");
        String template = "Hi {{first_name}}, welcome to {{company}}!";
        List<String> csvColumns = Arrays.asList("first_name", "last_name", "company");

        // When
        String result = emailRenderService.renderEmail(template, lead, csvColumns);

        // Then
        assertTrue(result.contains("John"));
        assertTrue(result.contains("Acme Corp"));
    }

    @Test
    void testRenderEmailWithSpintax() {
        // Given
        LeadEntity lead = createTestLead("Jane", "Smith", "Tech Inc");
        String template = "{Hi|Hello|Hey} {{first_name}}!";
        List<String> csvColumns = Arrays.asList("first_name");
        Long seed = 12345L; // Fixed seed for consistent results

        // When
        String result = emailRenderService.renderEmail(template, lead, csvColumns, seed);

        // Then
        assertTrue(result.contains("Jane"));
        assertTrue(result.matches("(Hi|Hello|Hey) Jane!"));
    }

    @Test
    void testRenderEmailWithNullTemplate() {
        // Given
        LeadEntity lead = createTestLead("John", "Doe", "Acme");
        List<String> csvColumns = Arrays.asList();

        // When
        String result = emailRenderService.renderEmail(null, lead, csvColumns);

        // Then
        assertEquals("", result);
    }

    @Test
    void testRenderEmailWithEmptyTemplate() {
        // Given
        LeadEntity lead = createTestLead("John", "Doe", "Acme");
        List<String> csvColumns = Arrays.asList();

        // When
        String result = emailRenderService.renderEmail("", lead, csvColumns);

        // Then
        assertEquals("", result);
    }

    @Test
    void testRenderEmailWithNullLead() {
        // Given
        String template = "Hi {{first_name}}!";
        List<String> csvColumns = Arrays.asList("first_name");

        // When
        String result = emailRenderService.renderEmail(template, null, csvColumns);

        // Then
        assertEquals(template, result); // Should return template as-is
    }

    @Test
    void testRenderEmailWithNullCsvColumns() {
        // Given
        LeadEntity lead = createTestLead("John", "Doe", "Acme");
        String template = "Hi {{first_name}}!";

        // When
        String result = emailRenderService.renderEmail(template, lead, null);

        // Then
        assertTrue(result.contains("John"));
    }

    @Test
    void testRenderEmailWithMissingVariable() {
        // Given
        LeadEntity lead = createTestLead("John", "Doe", "Acme");
        String template = "Hi {{first_name}}, your ID is {{missing_var}}!";
        List<String> csvColumns = Arrays.asList("first_name");

        // When
        String result = emailRenderService.renderEmail(template, lead, csvColumns);

        // Then
        assertTrue(result.contains("John"));
        // Missing variables are replaced with empty string
        assertFalse(result.contains("{{missing_var}}"));
    }

    @Test
    void testRenderEmailWithDifferentVariableFormats() {
        // Given
        LeadEntity lead = createTestLead("John", "Doe", "Acme Corp");
        String template = "{{First Name}} {{last_name}} at {{Company}}";
        List<String> csvColumns = Arrays.asList("first_name", "last_name", "company");

        // When
        String result = emailRenderService.renderEmail(template, lead, csvColumns);

        // Then
        assertTrue(result.contains("John"));
        assertTrue(result.contains("Doe"));
        assertTrue(result.contains("Acme Corp"));
    }

    @Test
    void testRenderEmailWithCsvData() {
        // Given
        LeadEntity lead = new LeadEntity();
        lead.setId(1);
        lead.setFirstName("Alice");
        lead.setLastName("Johnson");
        lead.setCompanyName("BigCorp");
        lead.setCsvData("{\"Custom Field\":\"Custom Value\",\"Department\":\"Engineering\"}");
        
        String template = "Hi {{first_name}}, you work in {{Department}} at {{company}}. {{Custom Field}}";
        List<String> csvColumns = Arrays.asList("first_name", "Department", "Custom Field");

        // When
        String result = emailRenderService.renderEmail(template, lead, csvColumns);

        // Then
        assertTrue(result.contains("Alice"));
        assertTrue(result.contains("Engineering"));
        assertTrue(result.contains("BigCorp"));
        assertTrue(result.contains("Custom Value"));
    }

    @Test
    void testRenderEmailWithComplexSpintax() {
        // Given
        LeadEntity lead = createTestLead("Bob", "Brown", "StartupXYZ");
        String template = "{Hi|Hello} {{first_name}}, {I noticed|I saw} you work at {{company}}.";
        List<String> csvColumns = Arrays.asList("first_name", "company");
        Long seed = 99999L;

        // When
        String result = emailRenderService.renderEmail(template, lead, csvColumns, seed);

        // Then
        assertTrue(result.contains("Bob"));
        assertTrue(result.contains("StartupXYZ"));
        assertFalse(result.contains("{"));
        assertFalse(result.contains("|"));
    }

    @Test
    void testRenderEmailWithEmailExtraction() {
        // Given
        LeadEntity lead = new LeadEntity();
        lead.setId(1);
        lead.setFirstName("Charlie");
        lead.setEmail("charlie@example-company.com");
        
        String template = "Hi {{first_name}} from {{company}}!";
        List<String> csvColumns = Arrays.asList("first_name");

        // When
        String result = emailRenderService.renderEmail(template, lead, csvColumns);

        // Then
        assertTrue(result.contains("Charlie"));
        assertTrue(result.contains("Example Company")); // Extracted from email domain
    }

    @Test
    void testParseCsvColumnsValid() {
        // Given
        String json = "[\"Name\",\"Email\",\"Company\"]";

        // When
        List<String> result = emailRenderService.parseCsvColumns(json);

        // Then
        assertEquals(3, result.size());
        assertTrue(result.contains("Name"));
        assertTrue(result.contains("Email"));
        assertTrue(result.contains("Company"));
    }

    @Test
    void testParseCsvColumnsNull() {
        // When
        List<String> result = emailRenderService.parseCsvColumns(null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testParseCsvColumnsEmpty() {
        // When
        List<String> result = emailRenderService.parseCsvColumns("");

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testParseCsvColumnsInvalidJson() {
        // Given
        String invalidJson = "not-valid-json";

        // When
        List<String> result = emailRenderService.parseCsvColumns(invalidJson);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testRenderEmailWithSpecialCharacters() {
        // Given
        LeadEntity lead = createTestLead("José", "García", "Café & Co");
        String template = "Hola {{first_name}} {{last_name}} de {{company}}!";
        List<String> csvColumns = Arrays.asList("first_name", "last_name", "company");

        // When
        String result = emailRenderService.renderEmail(template, lead, csvColumns);

        // Then
        assertTrue(result.contains("José"));
        assertTrue(result.contains("García"));
        assertTrue(result.contains("Café & Co"));
    }

    @Test
    void testRenderEmailWithDollarSign() {
        // Given
        LeadEntity lead = new LeadEntity();
        lead.setId(1);
        lead.setFirstName("John");
        lead.setCsvData("{\"Price\":\"$99.99\"}");
        
        String template = "Hi {{first_name}}, the price is {{Price}}";
        List<String> csvColumns = Arrays.asList("first_name", "Price");

        // When
        String result = emailRenderService.renderEmail(template, lead, csvColumns);

        // Then
        assertTrue(result.contains("John"));
        assertTrue(result.contains("$99.99"));
    }

    @Test
    void testRenderEmailWithMultipleSpintaxOptions() {
        // Given
        LeadEntity lead = createTestLead("Emma", "Wilson", "TechCorp");
        String template = "{Hey|Hi|Hello|Greetings} {{first_name}}!";
        List<String> csvColumns = Arrays.asList("first_name");
        Long seed = 54321L;

        // When
        String result = emailRenderService.renderEmail(template, lead, csvColumns, seed);

        // Then
        assertTrue(result.contains("Emma"));
        assertTrue(result.matches("(Hey|Hi|Hello|Greetings) Emma!"));
    }

    @Test
    void testRenderEmailWithNestedBraces() {
        // Given
        LeadEntity lead = createTestLead("David", "Lee", "InnovateCo");
        String template = "Hi {{first_name}}, {{company}} is great!";
        List<String> csvColumns = Arrays.asList("first_name", "company");

        // When
        String result = emailRenderService.renderEmail(template, lead, csvColumns);

        // Then
        assertTrue(result.contains("David"));
        assertTrue(result.contains("InnovateCo"));
        assertFalse(result.contains("{{"));
    }

    @Test
    void testRenderEmailWithJobTitle() {
        // Given
        LeadEntity lead = new LeadEntity();
        lead.setId(1);
        lead.setFirstName("Sarah");
        lead.setJobTitle("CEO");
        lead.setCompanyName("BigTech");
        
        String template = "Hi {{first_name}}, as {{position}} at {{company}}...";
        List<String> csvColumns = Arrays.asList("first_name", "position", "company");

        // When
        String result = emailRenderService.renderEmail(template, lead, csvColumns);

        // Then
        assertTrue(result.contains("Sarah"));
        assertTrue(result.contains("CEO"));
        assertTrue(result.contains("BigTech"));
    }

    @Test
    void testRenderEmailWithWebsite() {
        // Given
        LeadEntity lead = new LeadEntity();
        lead.setId(1);
        lead.setFirstName("Mike");
        lead.setCompanyWebsite("https://example.com");
        
        String template = "Visit {{website}} for more info, {{first_name}}!";
        List<String> csvColumns = Arrays.asList("first_name", "website");

        // When
        String result = emailRenderService.renderEmail(template, lead, csvColumns);

        // Then
        assertTrue(result.contains("Mike"));
        assertTrue(result.contains("https://example.com"));
    }

    @Test
    void testRenderEmailWithCountry() {
        // Given
        LeadEntity lead = new LeadEntity();
        lead.setId(1);
        lead.setFirstName("Anna");
        lead.setCountry("USA");
        
        String template = "Hi {{first_name}} from {{country}}!";
        List<String> csvColumns = Arrays.asList("first_name", "country");

        // When
        String result = emailRenderService.renderEmail(template, lead, csvColumns);

        // Then
        assertTrue(result.contains("Anna"));
        assertTrue(result.contains("USA"));
    }

    @Test
    void testRenderEmailWithCamelCaseVariables() {
        // Given
        LeadEntity lead = new LeadEntity();
        lead.setId(1);
        lead.setCsvData("{\"companyName\":\"TestCorp\",\"firstName\":\"Tom\"}");
        
        String template = "Hi {{firstName}} at {{companyName}}!";
        List<String> csvColumns = Arrays.asList("firstName", "companyName");

        // When
        String result = emailRenderService.renderEmail(template, lead, csvColumns);

        // Then
        assertTrue(result.contains("Tom"));
        assertTrue(result.contains("TestCorp"));
    }

    @Test
    void testRenderEmailWithUnderscoreVariables() {
        // Given
        LeadEntity lead = new LeadEntity();
        lead.setId(1);
        lead.setCsvData("{\"first_name\":\"Lisa\",\"company_name\":\"DevShop\"}");
        
        String template = "Hi {{first_name}} at {{company_name}}!";
        List<String> csvColumns = Arrays.asList("first_name", "company_name");

        // When
        String result = emailRenderService.renderEmail(template, lead, csvColumns);

        // Then
        assertTrue(result.contains("Lisa"));
        assertTrue(result.contains("DevShop"));
    }

    @Test
    void testRenderEmailWithSpaceVariables() {
        // Given
        LeadEntity lead = new LeadEntity();
        lead.setId(1);
        lead.setCsvData("{\"First Name\":\"Robert\",\"Company Name\":\"SpaceCo\"}");
        
        String template = "Hi {{First Name}} at {{Company Name}}!";
        List<String> csvColumns = Arrays.asList("First Name", "Company Name");

        // When
        String result = emailRenderService.renderEmail(template, lead, csvColumns);

        // Then
        assertTrue(result.contains("Robert"));
        assertTrue(result.contains("SpaceCo"));
    }

    // Helper method to create test lead
    private LeadEntity createTestLead(String firstName, String lastName, String company) {
        LeadEntity lead = new LeadEntity();
        lead.setId(1);
        lead.setFirstName(firstName);
        lead.setLastName(lastName);
        lead.setCompanyName(company);
        return lead;
    }
}
