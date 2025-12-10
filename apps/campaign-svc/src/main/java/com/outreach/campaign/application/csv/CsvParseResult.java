package com.outreach.campaign.application.csv;

import com.outreach.campaign.domain.models.Lead;
import java.util.List;
import java.util.Map;

public class CsvParseResult {
    private final List<Lead> leads;
    private final List<String> columns;
    private final List<Map<String, String>> rawRowData; // Full CSV row data as Map
    
    public CsvParseResult(List<Lead> leads, List<String> columns, List<Map<String, String>> rawRowData) {
        this.leads = leads;
        this.columns = columns;
        this.rawRowData = rawRowData;
    }
    
    public List<Lead> getLeads() {
        return leads;
    }
    
    public List<String> getColumns() {
        return columns;
    }
    
    public List<Map<String, String>> getRawRowData() {
        return rawRowData;
    }
}

