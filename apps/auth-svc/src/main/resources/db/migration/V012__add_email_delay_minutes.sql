-- Add email_delay_minutes column to campaigns table
-- This stores the delay in minutes between each email sent in a campaign
-- Minimum value is 5 minutes to avoid rate limiting

ALTER TABLE campaigns 
    ADD COLUMN IF NOT EXISTS email_delay_minutes INT DEFAULT 5;

-- Ensure minimum value constraint (at application level, but document it here)
-- The application will enforce a minimum of 5 minutes
COMMENT ON COLUMN campaigns.email_delay_minutes IS 'Delay in minutes between each email sent in the campaign. Minimum is 5 minutes to avoid rate limiting.';

