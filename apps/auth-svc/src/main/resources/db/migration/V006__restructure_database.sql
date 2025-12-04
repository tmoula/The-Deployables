-- Migration V006: Restructure database to new relationship model
-- This migration restructures the database to follow the new relationship pattern

-- ================
-- Step 1: Update users table structure
-- ================
-- Ensure users table has 'id' column (it may be 'user_id' or 'id' depending on migration state)
DO $$ 
BEGIN
    -- If user_id exists but id doesn't, rename it
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'users' AND column_name = 'user_id')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                       WHERE table_name = 'users' AND column_name = 'id') THEN
        ALTER TABLE users RENAME COLUMN user_id TO id;
    END IF;
    
    -- Ensure id is the primary key
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE table_name = 'users' AND constraint_name = 'users_pkey') THEN
        ALTER TABLE users ADD CONSTRAINT users_pkey PRIMARY KEY (id);
    END IF;
END $$;

-- ================
-- Step 2: Create company_profiles table (user's own company info)
-- ================
CREATE TABLE IF NOT EXISTS company_profiles (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    website VARCHAR(300),
    industry VARCHAR(150),
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_company_profiles_user ON company_profiles(user_id);

-- ================
-- Step 3: Update icp_profiles table structure
-- ================
-- Add new columns if they don't exist
ALTER TABLE icp_profiles 
    ADD COLUMN IF NOT EXISTS name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS geo_region VARCHAR(200),
    ADD COLUMN IF NOT EXISTS extra_notes TEXT;

-- Rename profile_name to name if profile_name exists
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'icp_profiles' AND column_name = 'profile_name') THEN
        -- Copy data from profile_name to name if name is null
        UPDATE icp_profiles SET name = profile_name WHERE name IS NULL;
        -- Drop profile_name after migration (commented for safety)
        -- ALTER TABLE icp_profiles DROP COLUMN profile_name;
    END IF;
END $$;

-- Note: Keeping icp_id column name for backward compatibility
-- The foreign key references will use icp_id

-- ================
-- Step 4: Create lead_batches table (each "Generate" click)
-- ================
CREATE TABLE IF NOT EXISTS lead_batches (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    icp_id INT REFERENCES icp_profiles(icp_id) ON DELETE SET NULL,
    source VARCHAR(100) DEFAULT 'manual',  -- e.g. "linkedin", "apollo", "scraper"
    status VARCHAR(50) DEFAULT 'pending',   -- "pending", "running", "ready", "failed"
    total_leads INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_lead_batches_user ON lead_batches(user_id);
CREATE INDEX idx_lead_batches_icp ON lead_batches(icp_id);
CREATE INDEX idx_lead_batches_status ON lead_batches(status);

-- ================
-- Step 5: Create new leads table (replaces companies/contacts structure)
-- ================
CREATE TABLE IF NOT EXISTS leads (
    id SERIAL PRIMARY KEY,
    batch_id INT REFERENCES lead_batches(id) ON DELETE CASCADE,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    company_name VARCHAR(200),
    company_website VARCHAR(300),
    job_title VARCHAR(200),
    email VARCHAR(250),
    linkedin_url VARCHAR(400),
    country VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_leads_batch ON leads(batch_id);
CREATE INDEX idx_leads_user ON leads(user_id);
CREATE INDEX idx_leads_email ON leads(email);
CREATE INDEX idx_leads_company ON leads(company_name);

-- ================
-- Step 6: Update campaigns table structure
-- ================
-- Add new columns
ALTER TABLE campaigns 
    ADD COLUMN IF NOT EXISTS from_mailbox_id BIGINT REFERENCES mailboxes(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS lead_batch_id INT REFERENCES lead_batches(id) ON DELETE SET NULL;

-- Note: Keeping campaign_id column name for backward compatibility

-- Rename campaign_name to name if needed
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'campaigns' AND column_name = 'campaign_name') THEN
        ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS name VARCHAR(200);
        UPDATE campaigns SET name = campaign_name WHERE name IS NULL;
        -- ALTER TABLE campaigns DROP COLUMN campaign_name; -- Commented for safety
    END IF;
END $$;

-- Keep icp_id for now but it will be replaced by lead_batch_id in the new flow
-- The icp_id can be derived from lead_batch_id -> lead_batches -> icp_id

