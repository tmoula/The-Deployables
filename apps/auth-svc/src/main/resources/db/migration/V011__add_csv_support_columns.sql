-- Migration: Add CSV support columns for campaigns and leads
-- This adds csv_data to leads (stores full CSV row as JSON) and csv_columns/csv_filename to campaigns

-- Add csv_data column to leads table (stores full CSV row data as JSON)
ALTER TABLE leads 
    ADD COLUMN IF NOT EXISTS csv_data TEXT;

-- Add csv_columns and csv_filename to campaigns table
ALTER TABLE campaigns 
    ADD COLUMN IF NOT EXISTS csv_columns TEXT,  -- JSON array of column names
    ADD COLUMN IF NOT EXISTS csv_filename VARCHAR(500);  -- Original CSV filename

-- Add email_subject and email_body to campaigns table (for storing email templates)
ALTER TABLE campaigns 
    ADD COLUMN IF NOT EXISTS email_subject TEXT,
    ADD COLUMN IF NOT EXISTS email_body TEXT;

-- Create index on csv_data for faster lookups (using GIN index for JSON queries if needed)
-- Note: PostgreSQL TEXT doesn't support GIN index directly, but we can add a functional index if needed
-- For now, we'll skip the index as TEXT search is already fast enough

COMMENT ON COLUMN leads.csv_data IS 'JSON string containing all CSV row data (column name -> value) for variable replacement';
COMMENT ON COLUMN campaigns.csv_columns IS 'JSON array of CSV column names (e.g., ["First Name", "Company Name", "Email"])';
COMMENT ON COLUMN campaigns.csv_filename IS 'Original CSV filename uploaded for this campaign';
COMMENT ON COLUMN campaigns.email_subject IS 'Email subject template with {{variables}}';
COMMENT ON COLUMN campaigns.email_body IS 'Email body template with {{variables}}';

