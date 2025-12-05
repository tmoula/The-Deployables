package com.outreach.campaign.application;

import com.outreach.campaign.domain.models.Lead;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CsvParserService {
    
    public List<Lead> parseCsv(MultipartFile file) {
        List<Lead> leads = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            // Read header line
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }
            
            String[] headers = parseCsvLine(headerLine);
            
            // Find column indices
            int matchScoreIdx = findColumnIndex(headers, "Match Score");
            int companyIdx = findColumnIndex(headers, "Company");
            int firstNameIdx = findColumnIndex(headers, "First Name");
            int lastNameIdx = findColumnIndex(headers, "Last Name");
            int positionIdx = findColumnIndex(headers, "Position");
            int emailIdx = findColumnIndex(headers, "Email");
            int domainIdx = findColumnIndex(headers, "Domain");
            int industryIdx = findColumnIndex(headers, "Industry");
            int companySizeIdx = findColumnIndex(headers, "Company Size");
            int regionsIdx = findColumnIndex(headers, "Regions");
            int techStackIdx = findColumnIndex(headers, "Tech Stack");
            int keywordsIdx = findColumnIndex(headers, "Keywords");
            int notesIdx = findColumnIndex(headers, "Notes");
            int personalizationHookIdx = findColumnIndex(headers, "Personalization Hook");
            
            // Read data rows
            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.trim().isEmpty()) continue;
                
                try {
                    String[] values = parseCsvLine(line);
                    
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
                    
                    Lead lead = new Lead(
                        UUID.randomUUID().toString(),
                        matchScore,
                        getValue(values, companyIdx),
                        getValue(values, firstNameIdx),
                        getValue(values, lastNameIdx),
                        getValue(values, positionIdx),
                        getValue(values, emailIdx),
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
                    // Continue processing other rows
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage(), e);
        }
        
        return leads;
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
    
    private String getValue(String[] values, int index) {
        if (index >= 0 && index < values.length && values[index] != null) {
            String value = values[index].trim();
            return value.isEmpty() ? null : value;
        }
        return null;
    }
}

