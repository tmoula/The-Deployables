package com.outreach.campaign.application.csv;

import com.outreach.campaign.domain.models.Lead;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CsvParserService {
    
    /**
     * Parse CSV file and extract:
     * 1. All column headers (for variables)
     * 2. All row data as Map (for storage and variable replacement)
     * 3. Lead objects (for backward compatibility)
     */
    public CsvParseResult parseCsv(MultipartFile file) {
        List<Lead> leads = new ArrayList<>();
        List<String> columns = new ArrayList<>();
        List<Map<String, String>> rawRowData = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            // Step 1: Read header line - Extract ALL column names
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }
            
            String[] headers = parseCsvLine(headerLine);
            // Store ALL column headers (trimmed and cleaned) - these become {{variables}}
            columns = Arrays.stream(headers)
                    .map(String::trim)
                    .filter(h -> !h.isEmpty())
                    .collect(Collectors.toList());
            
            System.out.println("CSV PARSER - Extracted " + columns.size() + " columns: " + columns);
            
            // Find column indices for standard fields (for Lead object creation)
            // Use flexible matching to find columns with variations
            int matchScoreIdx = findColumnIndex(headers, "Match Score");
            int companyIdx = findColumnIndexFlexible(headers, new String[]{"Company", "Company Name", "Company_Name", "companyName", "CompanyName"});
            int firstNameIdx = findColumnIndexFlexible(headers, new String[]{"First Name", "FirstName", "first_name", "First_Name"});
            int lastNameIdx = findColumnIndexFlexible(headers, new String[]{"Last Name", "LastName", "last_name", "Last_Name"});
            int positionIdx = findColumnIndexFlexible(headers, new String[]{"Position", "Job Title", "JobTitle", "job_title", "Job_Title", "Title"});
            int emailIdx = findColumnIndexFlexible(headers, new String[]{"Email", "E-mail", "email_address", "Email Address"});
            int domainIdx = findColumnIndexFlexible(headers, new String[]{"Domain", "Website", "Company Website", "CompanyWebsite"});
            int industryIdx = findColumnIndex(headers, "Industry");
            int companySizeIdx = findColumnIndexFlexible(headers, new String[]{"Company Size", "CompanySize", "company_size", "Size"});
            int regionsIdx = findColumnIndex(headers, "Regions");
            int techStackIdx = findColumnIndex(headers, "Tech Stack");
            int keywordsIdx = findColumnIndex(headers, "Keywords");
            int notesIdx = findColumnIndex(headers, "Notes");
            int personalizationHookIdx = findColumnIndex(headers, "Personalization Hook");
            
            // Step 2: Read data rows - Store ALL data as Map
            // IMPORTANT: Only count rows that have a valid email address
            String line;
            int rowNumber = 1;
            int skippedRows = 0;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.trim().isEmpty()) continue;
                
                try {
                    String[] values = parseCsvLine(line);
                    
                    // Step 3: Create Map with ALL CSV data (column name -> value)
                    // This is the key part - store everything, not just specific fields
                    Map<String, String> rowData = new LinkedHashMap<>();
                    for (int i = 0; i < headers.length && i < values.length; i++) {
                        String columnName = headers[i].trim();
                        String value = (i < values.length && values[i] != null) ? values[i].trim() : "";
                        rowData.put(columnName, value);
                    }
                    
                    // Extract email from row data - check multiple possible column names
                    String email = null;
                    // First, try the standard "Email" column index
                    if (emailIdx >= 0 && emailIdx < values.length) {
                        email = getValue(values, emailIdx);
                    }
                    // Also check rowData for email (case-insensitive) - handles variations like "email", "Email", "E-mail", etc.
                    if (email == null || email.trim().isEmpty()) {
                        for (Map.Entry<String, String> entry : rowData.entrySet()) {
                            String key = entry.getKey().trim();
                            if (key.equalsIgnoreCase("email") || 
                                key.equalsIgnoreCase("e-mail") ||
                                key.equalsIgnoreCase("email address") ||
                                key.equalsIgnoreCase("e-mail address") ||
                                key.toLowerCase().contains("email")) {
                                String value = entry.getValue();
                                if (value != null && !value.trim().isEmpty()) {
                                    email = value;
                                    break;
                                }
                            }
                        }
                    }
                    
                    // Only process rows that have a valid email address
                    if (email == null || email.trim().isEmpty() || !isValidEmail(email.trim())) {
                        skippedRows++;
                        System.out.println("Skipping row " + rowNumber + " - no valid email found");
                        continue;
                    }
                    
                    // Row has valid email - add to results
                    rawRowData.add(rowData);
                    
                    // Parse match score
                    Double matchScore = null;
                    if (matchScoreIdx >= 0 && matchScoreIdx < values.length && 
                        values[matchScoreIdx] != null && !values[matchScoreIdx].trim().isEmpty()) {
                        try {
                            matchScore = Double.parseDouble(values[matchScoreIdx].trim());
                        } catch (NumberFormatException e) {
                            // Ignore invalid numbers
                        }
                    }
                    
                    // Parse company size
                    Integer companySize = null;
                    if (companySizeIdx >= 0 && companySizeIdx < values.length && 
                        values[companySizeIdx] != null && !values[companySizeIdx].trim().isEmpty()) {
                        try {
                            companySize = Integer.parseInt(values[companySizeIdx].trim());
                        } catch (NumberFormatException e) {
                            // Ignore invalid numbers
                        }
                    }
                    
                    // Create Lead object (for backward compatibility)
                    Lead lead = new Lead(
                        UUID.randomUUID().toString(),
                        matchScore,
                        getValue(values, companyIdx),
                        getValue(values, firstNameIdx),
                        getValue(values, lastNameIdx),
                        getValue(values, positionIdx),
                        email.trim(), // Use the validated email
                        getValue(values, domainIdx),
                        getValue(values, industryIdx),
                        companySize,
                        getValue(values, regionsIdx),
                        getValue(values, techStackIdx),
                        getValue(values, keywordsIdx),
                        getValue(values, notesIdx),
                        getValue(values, personalizationHookIdx)
                    );
                    
                    leads.add(lead);
                } catch (Exception e) {
                    System.err.println("Error parsing row " + rowNumber + ": " + e.getMessage());
                    skippedRows++;
                    // Continue processing other rows
                }
            }
            
            System.out.println("CSV PARSER - Parsed " + leads.size() + " leads with valid emails (skipped " + skippedRows + " rows without emails)");
            
            System.out.println("CSV PARSER - Final count: " + leads.size() + " leads with valid emails, " + columns.size() + " columns detected");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage(), e);
        }
        
        return new CsvParseResult(leads, columns, rawRowData);
    }
    
    /**
     * Simple email validation - checks for basic email format
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        // Basic email validation: must contain @ and at least one dot after @
        String trimmed = email.trim().toLowerCase();
        int atIndex = trimmed.indexOf('@');
        if (atIndex <= 0 || atIndex >= trimmed.length() - 1) {
            return false;
        }
        String domain = trimmed.substring(atIndex + 1);
        return domain.contains(".") && domain.length() > 3; // e.g., "a.co" is minimum
    }
    
    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentValue = new StringBuilder();
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // Escaped quote
                    currentValue.append('"');
                    i++;
                } else {
                    // Toggle quote state
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // End of field
                values.add(currentValue.toString());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }
        
        // Add the last field
        values.add(currentValue.toString());
        
        return values.toArray(new String[0]);
    }
    
    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1; // Column not found
    }
    
    /**
     * Find column index by trying multiple possible column names (case-insensitive)
     * Returns the first match found
     */
    private int findColumnIndexFlexible(String[] headers, String[] possibleNames) {
        for (String name : possibleNames) {
            int idx = findColumnIndex(headers, name);
            if (idx >= 0) {
                return idx;
            }
        }
        return -1; // No match found
    }
    
    private String getValue(String[] values, int index) {
        if (index >= 0 && index < values.length && values[index] != null) {
            String value = values[index].trim();
            return value.isEmpty() ? null : value;
        }
        return null;
    }
}

