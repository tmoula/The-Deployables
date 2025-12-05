-- Migration: Simplify database to just users and leads tables
-- Remove all other tables: company_profiles, icp_profiles, lead_batches, campaigns, mailboxes, email_sequences, emails

-- Step 1: Drop all old tables first (in reverse dependency order)
DROP TABLE IF EXISTS emails CASCADE;
DROP TABLE IF EXISTS email_sequences CASCADE;
DROP TABLE IF EXISTS campaigns CASCADE;
DROP TABLE IF EXISTS mailboxes CASCADE;
DROP TABLE IF EXISTS lead_batches CASCADE;
DROP TABLE IF EXISTS icp_profiles CASCADE;
DROP TABLE IF EXISTS company_profiles CASCADE;
DROP TABLE IF EXISTS contacts CASCADE;
DROP TABLE IF EXISTS companies CASCADE;

-- Step 2: Drop leads table if it exists (we'll recreate it cleanly)
DROP TABLE IF EXISTS leads CASCADE;

-- Step 3: Create the leads table with all CSV fields
CREATE TABLE leads (
    id                  SERIAL PRIMARY KEY,
    user_id             INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    first_name          VARCHAR(100),
    last_name           VARCHAR(100),
    company_name        VARCHAR(200),
    company_link        VARCHAR(300),
    position            VARCHAR(200),
    email               VARCHAR(250),
    linkedin_url        VARCHAR(400),
    country             VARCHAR(100),
    personalization_hook TEXT,
    match_score         DECIMAL(5,2),
    industry            VARCHAR(150),
    company_size        INT,
    regions             VARCHAR(200),
    tech_stack          TEXT,
    keywords            TEXT,
    notes               TEXT,
    created_at          TIMESTAMP DEFAULT NOW()
);

-- Step 4: Create indexes for performance
CREATE INDEX idx_leads_user ON leads(user_id);
CREATE INDEX idx_leads_email ON leads(email);
CREATE INDEX idx_leads_company ON leads(company_name);

-- Final structure:
-- users table: id, email, password_hash, created_at
-- leads table: id, user_id, first_name, last_name, company_name, company_link, position, 
--              email, linkedin_url, country, personalization_hook, match_score, industry,
--              company_size, regions, tech_stack, keywords, notes, created_at

