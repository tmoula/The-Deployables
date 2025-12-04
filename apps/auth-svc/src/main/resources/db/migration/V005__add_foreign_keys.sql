-- Add foreign key constraints to existing tables
-- Note: V006 creates foreign keys inline for new tables (company_profiles, lead_batches, leads)

-- Ensure campaigns.user_id references users.id
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE constraint_name = 'fk_campaign_user' AND table_name = 'campaigns') THEN
        ALTER TABLE campaigns
        ADD CONSTRAINT fk_campaign_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Ensure icp_profiles.user_id references users.id
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints 
                   WHERE constraint_name = 'fk_icp_user' AND table_name = 'icp_profiles') THEN
        ALTER TABLE icp_profiles
        ADD CONSTRAINT fk_icp_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Note: Removed constraints for companies and contacts tables as they are being phased out
-- in favor of the new leads table structure (see V006)