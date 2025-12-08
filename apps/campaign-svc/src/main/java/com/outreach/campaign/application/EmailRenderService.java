package com.outreach.campaign.application;

import com.outreach.campaign.domain.entities.LeadEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
public class EmailRenderService {
    private final ObjectMapper objectMapper;
    
    public EmailRenderService() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Render email template by:
     * 1. Expanding spintax patterns {option1|option2|option3}
     * 2. Replacing {{variable}} placeholders with lead data
     * This works exactly like Instantly: uses CSV column names as variables
     * 
     * @param template The email template (subject or body)
     * @param lead The lead data to use for replacement
     * @param csvColumns List of CSV column names (for dynamic field access)
     * @param spintaxSeed Optional seed for spintax randomization (for consistent previews)
     * @return Rendered email with spintax expanded and variables replaced
     */
    public String renderEmail(String template, LeadEntity lead, java.util.List<String> csvColumns, Long spintaxSeed) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        
        System.out.println("RENDER EMAIL - Template: " + template.substring(0, Math.min(100, template.length())));
        System.out.println("RENDER EMAIL - Spintax seed: " + spintaxSeed);
        
        // Step 1: Expand spintax patterns {option1|option2|option3}
        // IMPORTANT: Preserve {{variable}} patterns (double braces) - only expand single brace spintax
        // This works with HTML content - it processes text between tags
        String result = expandSpintax(template, spintaxSeed);
        System.out.println("RENDER EMAIL - After spintax: " + result.substring(0, Math.min(100, result.length())));
        
        // Step 2: Build replacement map from lead data (including ALL CSV columns)
        Map<String, String> replacements = buildReplacementMap(lead, csvColumns);
        System.out.println("RENDER EMAIL - Replacement map size: " + replacements.size());
        System.out.println("RENDER EMAIL - Starting render for lead ID: " + lead.getId());
        System.out.println("RENDER EMAIL - Template preview: " + template.substring(0, Math.min(100, template.length())));
        System.out.println("RENDER EMAIL - CSV columns provided: " + csvColumns);
        System.out.println("RENDER EMAIL - Lead CSV data present: " + (lead.getCsvData() != null && !lead.getCsvData().trim().isEmpty()));
        System.out.println("RENDER EMAIL - Replacement map size: " + replacements.size());
        System.out.println("RENDER EMAIL - Sample replacements: " + replacements.entrySet().stream()
            .limit(10)
            .map(e -> e.getKey() + "=" + (e.getValue() != null ? e.getValue().substring(0, Math.min(20, e.getValue().length())) : "null"))
            .collect(java.util.stream.Collectors.joining(", ")));
        
        // Step 3: Replace all {{variable}} patterns
        // This regex works with HTML - it matches {{variable}} even inside HTML tags
        // Updated to handle spaces, underscores, and various formats: {{First Name}}, {{first_name}}, {{FirstName}}, {{First_Name}}
        // Allow underscores, letters, digits, and spaces in variable names
        Pattern pattern = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_\\s]+)\\s*\\}\\}");
        Matcher matcher = pattern.matcher(result);
        
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String variable = matcher.group(1).trim();
            System.out.println("RENDER EMAIL - Found variable in template: '{{" + variable + "}}'");
            String replacement = "";
            
            // Try multiple matching strategies
            // 1. Try with braces first: {{variable}}
            String keyWithBraces = "{{" + variable + "}}";
            if (replacements.containsKey(keyWithBraces)) {
                replacement = replacements.get(keyWithBraces);
                System.out.println("RENDER EMAIL - Found replacement via keyWithBraces: '" + keyWithBraces + "' = '" + replacement + "'");
            }
            // 2. Try without braces: variable
            else if (replacements.containsKey(variable)) {
                replacement = replacements.get(variable);
                System.out.println("RENDER EMAIL - Found replacement via variable key: '" + variable + "' = '" + replacement + "'");
            }
            // 3. Case-insensitive match (with braces)
            else {
                for (Map.Entry<String, String> entry : replacements.entrySet()) {
                    String key = entry.getKey();
                    // Remove {{ and }} from key for comparison
                    String keyVar = key.replaceAll("\\{\\{|\\}\\}", "");
                    if (keyVar.equalsIgnoreCase(variable)) {
                        replacement = entry.getValue();
                        break;
                    }
                }
            }
            // 4. Normalized match (spaces -> underscores, lowercase)
            if (replacement.isEmpty()) {
                String normalized = normalizeVariableName(variable);
                // Try normalized with braces
                if (replacements.containsKey("{{" + normalized + "}}")) {
                    replacement = replacements.get("{{" + normalized + "}}");
                }
                // Try normalized without braces
                else if (replacements.containsKey(normalized)) {
                    replacement = replacements.get(normalized);
                }
                // Try without underscores
                else {
                    String noUnderscore = normalized.replace("_", "");
                    if (replacements.containsKey("{{" + noUnderscore + "}}")) {
                        replacement = replacements.get("{{" + noUnderscore + "}}");
                    } else if (replacements.containsKey(noUnderscore)) {
                        replacement = replacements.get(noUnderscore);
                    }
                }
            }
            // 5. Try underscore version if variable has spaces (e.g., "First Name" -> "First_Name")
            if (replacement.isEmpty() && variable.contains(" ")) {
                String withUnderscore = variable.replace(" ", "_");
                if (replacements.containsKey("{{" + withUnderscore + "}}")) {
                    replacement = replacements.get("{{" + withUnderscore + "}}");
                } else if (replacements.containsKey(withUnderscore)) {
                    replacement = replacements.get(withUnderscore);
                }
            }
            // 6. Try space version if variable has underscores (e.g., "First_Name" -> "First Name")
            if (replacement.isEmpty() && variable.contains("_")) {
                String withSpace = variable.replace("_", " ");
                System.out.println("RENDER EMAIL - Trying space version: '" + withSpace + "'");
                if (replacements.containsKey("{{" + withSpace + "}}")) {
                    replacement = replacements.get("{{" + withSpace + "}}");
                    System.out.println("RENDER EMAIL - Found replacement via space version with braces: '" + replacement + "'");
                } else if (replacements.containsKey(withSpace)) {
                    replacement = replacements.get(withSpace);
                    System.out.println("RENDER EMAIL - Found replacement via space version: '" + replacement + "'");
                }
                // Also try case-insensitive
                if (replacement.isEmpty()) {
                    for (Map.Entry<String, String> entry : replacements.entrySet()) {
                        String key = entry.getKey();
                        String keyVar = key.replaceAll("\\{\\{|\\}\\}", "");
                        if (keyVar.equalsIgnoreCase(withSpace)) {
                            replacement = entry.getValue();
                            System.out.println("RENDER EMAIL - Found replacement via case-insensitive space match: '" + key + "' = '" + replacement + "'");
                            break;
                        }
                    }
                }
            }
            // 7. Try camelCase version if variable has underscores (e.g., "Company_Name" -> "companyName")
            if (replacement.isEmpty() && variable.contains("_")) {
                String[] parts = variable.split("_");
                if (parts.length > 1) {
                    // Convert to camelCase: "Company_Name" -> "companyName"
                    String camelCase = parts[0].toLowerCase() + 
                        Arrays.stream(parts).skip(1)
                            .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase())
                            .collect(java.util.stream.Collectors.joining());
                    if (replacements.containsKey("{{" + camelCase + "}}")) {
                        replacement = replacements.get("{{" + camelCase + "}}");
                    } else if (replacements.containsKey(camelCase)) {
                        replacement = replacements.get(camelCase);
                    }
                }
            }
            
            if (replacement.isEmpty()) {
                System.out.println("RENDER EMAIL - WARNING: No replacement found for {{" + variable + "}}");
                System.out.println("RENDER EMAIL - Tried keys: {{" + variable + "}}, " + variable + ", normalized variants");
                System.out.println("RENDER EMAIL - Available keys (first 20): " + replacements.keySet().stream()
                    .limit(20)
                    .collect(java.util.stream.Collectors.joining(", ")));
                System.out.println("RENDER EMAIL - Looking for keys containing 'First' or 'Company': " + 
                    replacements.keySet().stream()
                        .filter(k -> k.toLowerCase().contains("first") || k.toLowerCase().contains("company"))
                        .limit(10)
                        .collect(java.util.stream.Collectors.joining(", ")));
            } else {
                System.out.println("RENDER EMAIL - SUCCESS: Replacing {{" + variable + "}} with: '" + replacement + "'");
            }
            
            // Escape $ in replacement to avoid issues with Matcher.appendReplacement
            replacement = replacement.replace("$", "\\$");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        
        String finalResult = sb.toString();
        System.out.println("RENDER EMAIL - Final result preview: " + finalResult.substring(0, Math.min(200, finalResult.length())));
        
        return finalResult;
    }
    
    /**
     * Overload without spintax seed (uses random seed)
     */
    public String renderEmail(String template, LeadEntity lead, java.util.List<String> csvColumns) {
        return renderEmail(template, lead, csvColumns, System.currentTimeMillis());
    }
    
    /**
     * Expand spintax patterns: {option1|option2|option3} -> randomly pick one
     * Preserves {{variable}} patterns (double braces)
     * 
     * @param text The text containing spintax patterns
     * @param seed Seed for randomization (null = random)
     * @return Text with spintax expanded
     */
    private String expandSpintax(String text, Long seed) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // Use seed for consistent randomization, or current time if null
        long randomSeed = (seed != null) ? seed : System.currentTimeMillis();
        java.util.Random random = new java.util.Random(randomSeed);
        
        // First, temporarily replace {{variable}} with placeholders to protect them
        Map<String, String> variablePlaceholders = new HashMap<>();
        int placeholderIndex = 0;
        String protectedText = text;
        
        // Protect all {{variable}} patterns
        Pattern varPattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
        Matcher varMatcher = varPattern.matcher(protectedText);
        StringBuffer varBuffer = new StringBuffer();
        while (varMatcher.find()) {
            String placeholder = "__VAR_PLACEHOLDER_" + placeholderIndex + "__";
            variablePlaceholders.put(placeholder, varMatcher.group(0));
            varMatcher.appendReplacement(varBuffer, Matcher.quoteReplacement(placeholder));
            placeholderIndex++;
        }
        varMatcher.appendTail(varBuffer);
        protectedText = varBuffer.toString();
        
        // Now expand spintax patterns (single braces only)
        Pattern spintaxPattern = Pattern.compile("\\{([^}]+)\\}"); // Single braces
        Matcher spintaxMatcher = spintaxPattern.matcher(protectedText);
        StringBuffer spintaxBuffer = new StringBuffer();
        
        while (spintaxMatcher.find()) {
            String options = spintaxMatcher.group(1);
            String[] choices = options.split("\\|");
            if (choices.length > 0) {
                // Trim each option
                for (int i = 0; i < choices.length; i++) {
                    choices[i] = choices[i].trim();
                }
                // Randomly select one
                String selected = choices[random.nextInt(choices.length)];
                spintaxMatcher.appendReplacement(spintaxBuffer, Matcher.quoteReplacement(selected));
            } else {
                // No options found, keep original
                spintaxMatcher.appendReplacement(spintaxBuffer, Matcher.quoteReplacement(spintaxMatcher.group(0)));
            }
        }
        spintaxMatcher.appendTail(spintaxBuffer);
        String expanded = spintaxBuffer.toString();
        
        // Restore {{variable}} patterns
        for (Map.Entry<String, String> entry : variablePlaceholders.entrySet()) {
            expanded = expanded.replace(entry.getKey(), entry.getValue());
        }
        
        return expanded;
    }
    
    /**
     * Normalize variable name for matching (e.g., "First Name" -> "first_name", "firstname")
     */
    private String normalizeVariableName(String variable) {
        return variable.toLowerCase()
            .replaceAll("\\s+", "_")
            .replaceAll("[^a-z0-9_]", "");
    }
    
    /**
     * Build a map of variable names to values from lead data
     * STEP 5: Use JSON data for ALL CSV column variables (like Instantly)
     */
    private Map<String, String> buildReplacementMap(LeadEntity lead, java.util.List<String> csvColumns) {
        Map<String, String> map = new HashMap<>();
        
        // STEP 1: Load CSV row data from JSON (stored in csv_data field)
        Map<String, String> csvData = null;
        if (lead.getCsvData() != null && !lead.getCsvData().trim().isEmpty()) {
            try {
                csvData = objectMapper.readValue(lead.getCsvData(), new TypeReference<Map<String, String>>() {});
                System.out.println("BUILD REPLACEMENT MAP - Loaded CSV data for lead " + lead.getId() + ": " + csvData.size() + " fields");
                System.out.println("BUILD REPLACEMENT MAP - CSV column names: " + csvData.keySet());
                System.out.println("BUILD REPLACEMENT MAP - CSV data sample: " + csvData.entrySet().stream()
                    .limit(5)
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(java.util.stream.Collectors.joining(", ")));
            } catch (Exception e) {
                System.err.println("BUILD REPLACEMENT MAP - ERROR parsing CSV data JSON for lead " + lead.getId() + ": " + e.getMessage());
                e.printStackTrace();
                System.err.println("BUILD REPLACEMENT MAP - Raw CSV data (first 500 chars): " + 
                    lead.getCsvData().substring(0, Math.min(500, lead.getCsvData().length())));
            }
        } else {
            System.err.println("BUILD REPLACEMENT MAP - CRITICAL WARNING: No CSV data found for lead " + lead.getId() + " (csv_data is null or empty)");
            System.err.println("BUILD REPLACEMENT MAP - This lead was likely generated by AI, not imported from CSV!");
            System.err.println("BUILD REPLACEMENT MAP - Only standard fields (firstName, lastName, etc.) will be available for replacement.");
        }
        
        // STEP 2: Add ALL CSV columns as variables (this is the key part!)
        if (csvData != null) {
            for (Map.Entry<String, String> entry : csvData.entrySet()) {
                String columnName = entry.getKey();
                String value = entry.getValue() != null ? entry.getValue() : "";
                
                System.out.println("BUILD REPLACEMENT MAP - Processing column: '" + columnName + "' = '" + value + "'");
                
                // Add with original column name (WITHOUT braces for direct lookup)
                map.put(columnName, value);
                
                // Add WITH braces for {{variable}} matching
                map.put("{{" + columnName + "}}", value);
                
                // Add normalized versions for flexible matching
                String normalized = normalizeVariableName(columnName);
                map.put(normalized, value);
                map.put("{{" + normalized + "}}", value);
                
                // Add without underscores
                String noUnderscore = normalized.replace("_", "");
                map.put(noUnderscore, value);
                map.put("{{" + noUnderscore + "}}", value);
                
                // Add underscore version if original has spaces (e.g., "First Name" -> "First_Name")
                if (columnName.contains(" ")) {
                    String withUnderscore = columnName.replace(" ", "_");
                    map.put(withUnderscore, value);
                    map.put("{{" + withUnderscore + "}}", value);
                    System.out.println("BUILD REPLACEMENT MAP - Added underscore variant: '" + withUnderscore + "'");
                    
                    // Also add with different capitalizations
                    String withUnderscoreLower = withUnderscore.toLowerCase();
                    map.put(withUnderscoreLower, value);
                    map.put("{{" + withUnderscoreLower + "}}", value);
                    
                    String withUnderscoreUpper = withUnderscore.toUpperCase();
                    map.put(withUnderscoreUpper, value);
                    map.put("{{" + withUnderscoreUpper + "}}", value);
                    
                    // CamelCase version (e.g., "First Name" -> "FirstName")
                    String camelCase = Arrays.stream(columnName.split("\\s+"))
                        .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                        .collect(java.util.stream.Collectors.joining());
                    map.put(camelCase, value);
                    map.put("{{" + camelCase + "}}", value);
                }
                
                // Add space version if original has underscores (e.g., "First_Name" -> "First Name")
                if (columnName.contains("_")) {
                    String withSpace = columnName.replace("_", " ");
                    map.put(withSpace, value);
                    map.put("{{" + withSpace + "}}", value);
                    System.out.println("BUILD REPLACEMENT MAP - Added space variant: '" + withSpace + "'");
                }
                
                // Handle camelCase -> underscore format (e.g., "companyName" -> "Company_Name")
                // Check if columnName is camelCase (has lowercase followed by uppercase)
                if (columnName.matches(".*[a-z][A-Z].*")) {
                    // Split camelCase: "companyName" -> ["company", "Name"]
                    String[] parts = columnName.split("(?=[A-Z])");
                    if (parts.length > 1) {
                        // Join with underscore and capitalize first letter of each part
                        String withUnderscore = Arrays.stream(parts)
                            .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase())
                            .collect(java.util.stream.Collectors.joining("_"));
                        map.put(withUnderscore, value);
                        map.put("{{" + withUnderscore + "}}", value);
                        System.out.println("BUILD REPLACEMENT MAP - Added camelCase->underscore variant: '" + withUnderscore + "'");
                        
                        // Also add lowercase version: "company_name"
                        String withUnderscoreLower = Arrays.stream(parts)
                            .map(String::toLowerCase)
                            .collect(java.util.stream.Collectors.joining("_"));
                        map.put(withUnderscoreLower, value);
                        map.put("{{" + withUnderscoreLower + "}}", value);
                    }
                }
                
                // Add capitalized versions
                if (columnName.length() > 0) {
                    String capitalized = columnName.substring(0, 1).toUpperCase() + columnName.substring(1);
                    map.put(capitalized, value);
                    map.put("{{" + capitalized + "}}", value);
                }
            }
            System.out.println("BUILD REPLACEMENT MAP - Total replacement keys created: " + map.size());
        }
        
        // STEP 3: Add standard field mappings (for backward compatibility)
        // Add both with and without {{}} braces for flexible matching
        if (lead.getFirstName() != null) {
            String firstName = lead.getFirstName();
            map.put("{{first_name}}", firstName);
            map.put("{{firstname}}", firstName);
            map.put("{{firstName}}", firstName);
            map.put("{{First Name}}", firstName);
            map.put("first_name", firstName);
            map.put("firstname", firstName);
            map.put("firstName", firstName);
            map.put("First Name", firstName);
        }
        if (lead.getLastName() != null) {
            String lastName = lead.getLastName();
            map.put("{{last_name}}", lastName);
            map.put("{{lastname}}", lastName);
            map.put("{{lastName}}", lastName);
            map.put("{{Last Name}}", lastName);
            map.put("last_name", lastName);
            map.put("lastname", lastName);
            map.put("lastName", lastName);
            map.put("Last Name", lastName);
        }
        if (lead.getCompanyName() != null) {
            String company = lead.getCompanyName();
            map.put("{{company}}", company);
            map.put("{{company_name}}", company);
            map.put("{{Company_Name}}", company);  // User's preferred format
            map.put("{{Company}}", company);
            map.put("{{Company Name}}", company);
            map.put("{{Company name}}", company);
            map.put("company", company);
            map.put("company_name", company);
            map.put("Company", company);
            map.put("Company Name", company);
        }
        if (lead.getJobTitle() != null) {
            String jobTitle = lead.getJobTitle();
            map.put("{{position}}", jobTitle);
            map.put("{{job_title}}", jobTitle);
            map.put("{{title}}", jobTitle);
            map.put("{{Position}}", jobTitle);
            map.put("position", jobTitle);
            map.put("job_title", jobTitle);
            map.put("title", jobTitle);
            map.put("Position", jobTitle);
        }
        if (lead.getEmail() != null) {
            String email = lead.getEmail();
            map.put("{{email}}", email);
            map.put("{{Email}}", email);
            map.put("email", email);
            map.put("Email", email);
        }
        if (lead.getCompanyWebsite() != null) {
            String website = lead.getCompanyWebsite();
            map.put("{{domain}}", website);
            map.put("{{website}}", website);
            map.put("{{Domain}}", website);
            map.put("domain", website);
            map.put("website", website);
            map.put("Domain", website);
        }
        if (lead.getCountry() != null) {
            String country = lead.getCountry();
            map.put("{{country}}", country);
            map.put("{{region}}", country);
            map.put("country", country);
            map.put("region", country);
        }
        
        // Parse custom_notes for additional fields (stored as "Field: value; Field: value")
        if (lead.getCustomNotes() != null) {
            String[] parts = lead.getCustomNotes().split(";");
            for (String part : parts) {
                if (part.contains(":")) {
                    String[] keyValue = part.split(":", 2);
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim().toLowerCase();
                        String value = keyValue[1].trim();
                        map.put(key, value);
                        // Also add with underscores
                        map.put(key.replace(" ", "_"), value);
                    }
                }
            }
        }
        
        return map;
    }
}

