-- Force drop tables: sender_companies, icp_profiles, lead_batches
-- This script handles all foreign key dependencies

-- Step 1: Drop foreign key constraints that reference these tables
ALTER TABLE IF EXISTS campaigns DROP CONSTRAINT IF EXISTS campaigns_icp_id_fkey;
ALTER TABLE IF EXISTS campaigns DROP CONSTRAINT IF EXISTS campaigns_lead_batch_id_fkey;
ALTER TABLE IF EXISTS lead_batches DROP CONSTRAINT IF EXISTS lead_batches_icp_id_fkey;
ALTER TABLE IF EXISTS leads DROP CONSTRAINT IF EXISTS leads_batch_id_fkey;

-- Step 2: Drop indexes
DROP INDEX IF EXISTS idx_sender_companies_user CASCADE;
DROP INDEX IF EXISTS idx_icp_profiles_user CASCADE;
DROP INDEX IF EXISTS idx_lead_batches_user CASCADE;
DROP INDEX IF EXISTS idx_lead_batches_icp CASCADE;
DROP INDEX IF EXISTS idx_lead_batches_status CASCADE;
DROP INDEX IF EXISTS idx_campaigns_icp CASCADE;
DROP INDEX IF EXISTS idx_campaigns_lead_batch CASCADE;
DROP INDEX IF EXISTS idx_leads_batch CASCADE;

-- Step 3: Drop the tables (CASCADE will handle any remaining dependencies)
DROP TABLE IF EXISTS sender_companies CASCADE;
DROP TABLE IF EXISTS lead_batches CASCADE;
DROP TABLE IF EXISTS icp_profiles CASCADE;

-- Step 4: Verify they're gone
SELECT 'Tables dropped. Remaining:' as status;
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name IN ('sender_companies', 'icp_profiles', 'lead_batches');

