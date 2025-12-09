-- Drop unused tables: sender_companies, icp_profiles, lead_batches
-- These tables are not needed for the current CSV-based campaign flow

-- Drop indexes first (optional, but cleaner)
DROP INDEX IF EXISTS idx_sender_companies_user;
DROP INDEX IF EXISTS idx_icp_profiles_user;
DROP INDEX IF EXISTS idx_lead_batches_user;
DROP INDEX IF EXISTS idx_lead_batches_icp;
DROP INDEX IF EXISTS idx_lead_batches_status;
DROP INDEX IF EXISTS idx_campaigns_icp;
DROP INDEX IF EXISTS idx_campaigns_lead_batch;
DROP INDEX IF EXISTS idx_leads_batch;

-- Drop tables (foreign keys will be automatically dropped)
-- Order matters: drop tables that reference others first
DROP TABLE IF EXISTS sender_companies CASCADE;
DROP TABLE IF EXISTS lead_batches CASCADE;
DROP TABLE IF EXISTS icp_profiles CASCADE;

-- Note: The foreign key columns in other tables (campaigns.icp_id, campaigns.lead_batch_id, leads.batch_id)
-- will remain but can be NULL. If you want to remove these columns too, uncomment below:
-- ALTER TABLE campaigns DROP COLUMN IF EXISTS icp_id;
-- ALTER TABLE campaigns DROP COLUMN IF EXISTS lead_batch_id;
-- ALTER TABLE leads DROP COLUMN IF EXISTS batch_id;

