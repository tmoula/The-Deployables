package com.outreach.campaign.application;

import com.outreach.campaign.domain.entities.LeadEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
        
        if (lead == null) {
            System.err.println("RENDER EMAIL - ERROR: Lead is null!");
            return template; // Return template as-is if no lead data
        }
        
        System.out.println("RENDER EMAIL - Template: " + template.substring(0, Math.min(100, template.length())));
        System.out.println("RENDER EMAIL - Spintax seed: " + spintaxSeed);
        System.out.println("RENDER EMAIL - Lead ID: " + lead.getId());
        System.out.println("RENDER EMAIL - Lead has CSV data: " + (lead.getCsvData() != null && !lead.getCsvData().trim().isEmpty()));
        
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
            
            // Generate ALL possible variations of the variable to try
            List<String> variationsToTry = new ArrayList<>();
            
            // 1. Original variable (exact)
            variationsToTry.add(variable);
            variationsToTry.add("{{" + variable + "}}");
            
            // 2. Normalized (lowercase, spaces -> underscores)
            String normalized = normalizeVariableName(variable);
            variationsToTry.add(normalized);
            variationsToTry.add("{{" + normalized + "}}");
            
            // 3. Space <-> Underscore conversions
            if (variable.contains("_")) {
                String withSpace = variable.replace("_", " ");
                variationsToTry.add(withSpace);
                variationsToTry.add("{{" + withSpace + "}}");
                // Case variations
                variationsToTry.add(withSpace.toLowerCase());
                variationsToTry.add("{{" + withSpace.toLowerCase() + "}}");
                variationsToTry.add(capitalizeWords(withSpace));
                variationsToTry.add("{{" + capitalizeWords(withSpace) + "}}");
            }
            if (variable.contains(" ")) {
                String withUnderscore = variable.replace(" ", "_");
                variationsToTry.add(withUnderscore);
                variationsToTry.add("{{" + withUnderscore + "}}");
                // Case variations
                variationsToTry.add(withUnderscore.toLowerCase());
                variationsToTry.add("{{" + withUnderscore.toLowerCase() + "}}");
                variationsToTry.add(withUnderscore.toUpperCase());
                variationsToTry.add("{{" + withUnderscore.toUpperCase() + "}}");
            }
            
            // 4. CamelCase conversions
            if (variable.contains("_")) {
                String[] parts = variable.split("_");
                if (parts.length > 1) {
                    // camelCase: "first_name" -> "firstName"
                    String camelCase = parts[0].toLowerCase() + 
                        Arrays.stream(parts).skip(1)
                            .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase())
                            .collect(java.util.stream.Collectors.joining());
                    variationsToTry.add(camelCase);
                    variationsToTry.add("{{" + camelCase + "}}");
                    // PascalCase: "First_Name" -> "FirstName"
                    String pascalCase = Arrays.stream(parts)
                        .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase())
                        .collect(java.util.stream.Collectors.joining());
                    variationsToTry.add(pascalCase);
                    variationsToTry.add("{{" + pascalCase + "}}");
                }
            }
            
            // 5. Case variations of original
            variationsToTry.add(variable.toLowerCase());
            variationsToTry.add("{{" + variable.toLowerCase() + "}}");
            variationsToTry.add(variable.toUpperCase());
            variationsToTry.add("{{" + variable.toUpperCase() + "}}");
            if (variable.length() > 0) {
                String capitalized = variable.substring(0, 1).toUpperCase() + variable.substring(1).toLowerCase();
                variationsToTry.add(capitalized);
                variationsToTry.add("{{" + capitalized + "}}");
            }
            
            // Remove duplicates
            variationsToTry = variationsToTry.stream().distinct().collect(java.util.stream.Collectors.toList());
            
            System.out.println("RENDER EMAIL - Trying " + variationsToTry.size() + " variations for '{{" + variable + "}}'");
            
            // Try each variation
            for (String variation : variationsToTry) {
                if (replacements.containsKey(variation)) {
                    replacement = replacements.get(variation);
                    System.out.println("RENDER EMAIL - SUCCESS: Found match for '{{" + variable + "}}' using variation '" + variation + "' = '" + replacement + "'");
                    break;
                }
            }
            
            // 6. Last resort: Case-insensitive search through all keys
            if (replacement.isEmpty()) {
                String variableLower = variable.toLowerCase().replaceAll("[^a-z0-9]", "");
                for (Map.Entry<String, String> entry : replacements.entrySet()) {
                    String key = entry.getKey().replaceAll("\\{\\{|\\}\\}", "").toLowerCase().replaceAll("[^a-z0-9]", "");
                    if (key.equals(variableLower)) {
                        replacement = entry.getValue();
                        System.out.println("RENDER EMAIL - SUCCESS: Found case-insensitive match for '{{" + variable + "}}' using key '" + entry.getKey() + "' = '" + replacement + "'");
                        break;
                    }
                }
            }
            
            if (replacement.isEmpty()) {
                System.out.println("RENDER EMAIL - WARNING: No replacement found for {{" + variable + "}}");
                System.out.println("RENDER EMAIL - Tried " + variationsToTry.size() + " variations: " + 
                    variationsToTry.stream().limit(10).collect(java.util.stream.Collectors.joining(", ")) + "...");
                System.out.println("RENDER EMAIL - Available keys (first 30): " + replacements.keySet().stream()
                    .limit(30)
                    .collect(java.util.stream.Collectors.joining(", ")));
                System.out.println("RENDER EMAIL - Looking for keys containing 'company' (case-insensitive): " + 
                    replacements.keySet().stream()
                        .filter(k -> k.toLowerCase().contains("company"))
                        .limit(20)
                        .collect(java.util.stream.Collectors.joining(", ")));
                System.out.println("RENDER EMAIL - Looking for keys containing 'first' (case-insensitive): " + 
                    replacements.keySet().stream()
                        .filter(k -> k.toLowerCase().contains("first"))
                        .limit(20)
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
     * Capitalize first letter of each word (e.g., "first name" -> "First Name")
     */
    private String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) return text;
        return Arrays.stream(text.split("\\s+"))
            .map(word -> word.length() > 0 
                ? word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase()
                : word)
            .collect(java.util.stream.Collectors.joining(" "));
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
        
        // Try to get company name from lead entity first, then from CSV data, then from email domain
        String companyName = lead.getCompanyName();
        if ((companyName == null || companyName.trim().isEmpty()) && csvData != null) {
            // Try to find company name in CSV data using various keys
            // Check for exact matches first (case-insensitive)
            String[] companyKeys = {"Company Name", "Company", "Company_Name", "companyName", "CompanyName", 
                                    "Company name", "company_name", "COMPANY", "Company_Name", "COMPANY_NAME",
                                    "company", "CompanyName", "COMPANY_NAME", "Company Name", "company name"};
            
            // First pass: exact case-insensitive match
            for (String key : companyKeys) {
                for (String csvKey : csvData.keySet()) {
                    if (csvKey.equalsIgnoreCase(key.trim())) {
                        companyName = csvData.get(csvKey);
                        if (companyName != null && !companyName.trim().isEmpty()) {
                            System.out.println("BUILD REPLACEMENT MAP - Found company name in CSV data using exact match: '" + csvKey + "' = '" + companyName + "'");
                            break;
                        }
                    }
                }
                if (companyName != null && !companyName.trim().isEmpty()) {
                    break;
                }
            }
            
            // Second pass: partial matches (contains "company" but not size/website/assignee)
            if (companyName == null || companyName.trim().isEmpty()) {
                for (String key : csvData.keySet()) {
                    String lowerKey = key.toLowerCase().trim();
                    // Check if key contains "company" but exclude unwanted variations
                    if (lowerKey.contains("company") && 
                        !lowerKey.contains("size") && 
                        !lowerKey.contains("website") && 
                        !lowerKey.contains("assignee") &&
                        !lowerKey.contains("email") &&
                        !lowerKey.contains("provider")) {
                        companyName = csvData.get(key);
                        if (companyName != null && !companyName.trim().isEmpty()) {
                            System.out.println("BUILD REPLACEMENT MAP - Found company name in CSV data using partial match: '" + key + "' = '" + companyName + "'");
                            break;
                        }
                    }
                }
            }
            
            // Debug: log all CSV keys if company name still not found
            if (companyName == null || companyName.trim().isEmpty()) {
                System.out.println("BUILD REPLACEMENT MAP - WARNING: Could not find company name. Available CSV keys: " + csvData.keySet());
            }
        }
        
        // Last resort: Extract company name from email domain if still not found
        if ((companyName == null || companyName.trim().isEmpty()) && lead.getEmail() != null) {
            String email = lead.getEmail().trim();
            int atIndex = email.indexOf('@');
            if (atIndex > 0 && atIndex < email.length() - 1) {
                String domain = email.substring(atIndex + 1);
                // Remove common TLDs and extract company name
                String domainWithoutTld = domain.replaceFirst("\\.(com|net|org|io|co|ai|app|dev|tech|xyz|uk|ca|au|de|fr|jp|cn)$", "");
                
                // Handle subdomains (e.g., "mail.company.com" -> "company")
                if (domainWithoutTld.contains(".")) {
                    String[] parts = domainWithoutTld.split("\\.");
                    // Usually the company name is the second-to-last or last part
                    domainWithoutTld = parts[parts.length - 1]; // Take the last part (main domain)
                }
                
                // Convert to title case
                if (domainWithoutTld.contains("-") || domainWithoutTld.contains("_")) {
                    // Split on dashes/underscores: "modern-adventure" -> "Modern Adventure"
                    String separator = domainWithoutTld.contains("-") ? "-" : "_";
                    String[] parts = domainWithoutTld.split(separator);
                    companyName = Arrays.stream(parts)
                        .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase())
                        .collect(java.util.stream.Collectors.joining(" "));
                } else {
                    // Try to detect camelCase and split it: "modernAdventure" -> "Modern Adventure"
                    String withSpaces = domainWithoutTld.replaceAll("([a-z])([A-Z])", "$1 $2");
                    // Capitalize first letter of each word
                    String[] words = withSpaces.split("\\s+");
                    companyName = Arrays.stream(words)
                        .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                        .collect(java.util.stream.Collectors.joining(" "));
                }
                System.out.println("BUILD REPLACEMENT MAP - Extracted company name from email domain: '" + email + "' -> '" + companyName + "'");
            }
        }
        
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
        if (companyName != null && !companyName.trim().isEmpty()) {
            String company = companyName;
            map.put("{{company}}", company);
            map.put("{{company_name}}", company);
            map.put("{{Company_Name}}", company);  // User's preferred format
            map.put("{{Company}}", company);
            map.put("{{Company Name}}", company);
            map.put("{{Company name}}", company);
            map.put("{{companyName}}", company);  // camelCase version
            map.put("company", company);
            map.put("company_name", company);
            map.put("Company", company);
            map.put("Company Name", company);
            map.put("companyName", company);  // camelCase version
            System.out.println("BUILD REPLACEMENT MAP - Added company name mappings: '" + company + "'");
        } else {
            System.err.println("BUILD REPLACEMENT MAP - WARNING: No company name found in lead entity or CSV data!");
            System.err.println("BUILD REPLACEMENT MAP - Lead entity companyName: " + lead.getCompanyName());
            if (csvData != null) {
                System.err.println("BUILD REPLACEMENT MAP - CSV data keys: " + csvData.keySet());
            }
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

