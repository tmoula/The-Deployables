-- Migration: Update leads table to support CSV imports with all fields
-- This replaces the companies/contacts approach with a unified leads table

-- Add missing columns to leads table if they don't exist
DO $$ 
BEGIN
    -- Add campaign_id to link leads to campaigns (nullable for now, will be set during import)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'leads' AND column_name = 'campaign_id') THEN
        ALTER TABLE leads ADD COLUMN campaign_id INT REFERENCES campaigns(campaign_id) ON DELETE CASCADE;
        CREATE INDEX idx_leads_campaign ON leads(campaign_id);
    END IF;

    -- Add personalization_hook (from CSV Personalization Hook column)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'leads' AND column_name = 'personalization_hook') THEN
        ALTER TABLE leads ADD COLUMN personalization_hook TEXT;
    END IF;

    -- Add match_score (from CSV Match Score column)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'leads' AND column_name = 'match_score') THEN
        ALTER TABLE leads ADD COLUMN match_score DECIMAL(5,2);
    END IF;

    -- Add industry (from CSV Industry column)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'leads' AND column_name = 'industry') THEN
        ALTER TABLE leads ADD COLUMN industry VARCHAR(150);
    END IF;

    -- Add company_size (from CSV Company Size column)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'leads' AND column_name = 'company_size') THEN
        ALTER TABLE leads ADD COLUMN company_size INT;
    END IF;

    -- Add regions (from CSV Regions column)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'leads' AND column_name = 'regions') THEN
        ALTER TABLE leads ADD COLUMN regions VARCHAR(200);
    END IF;

    -- Add tech_stack (from CSV Tech Stack column)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'leads' AND column_name = 'tech_stack') THEN
        ALTER TABLE leads ADD COLUMN tech_stack TEXT;
    END IF;

    -- Add keywords (from CSV Keywords column)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'leads' AND column_name = 'keywords') THEN
        ALTER TABLE leads ADD COLUMN keywords TEXT;
    END IF;

    -- Add notes (from CSV Notes column)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'leads' AND column_name = 'notes') THEN
        ALTER TABLE leads ADD COLUMN notes TEXT;
    END IF;

    -- Rename company_website to company_link for consistency (if exists)
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'leads' AND column_name = 'company_website') THEN
        ALTER TABLE leads RENAME COLUMN company_website TO company_link;
    END IF;

    -- Rename job_title to position for consistency (if exists)
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'leads' AND column_name = 'job_title') THEN
        ALTER TABLE leads RENAME COLUMN job_title TO position;
    END IF;
END $$;

-- Create index on campaign_id for faster lookups
CREATE INDEX IF NOT EXISTS idx_leads_campaign_id ON leads(campaign_id);

-- Note: batch_id can remain for AI-generated leads, but campaign_id will be used for CSV imports


