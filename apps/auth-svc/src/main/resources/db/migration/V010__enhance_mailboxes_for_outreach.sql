-- Migration: Enhance mailboxes table for outreach tool architecture
-- Adds SMTP/IMAP configuration, OAuth tokens, quotas, and metadata

-- Add SMTP/IMAP connection fields
ALTER TABLE mailboxes 
    ADD COLUMN IF NOT EXISTS smtp_host VARCHAR(255),
    ADD COLUMN IF NOT EXISTS smtp_port INTEGER DEFAULT 587,
    ADD COLUMN IF NOT EXISTS imap_host VARCHAR(255),
    ADD COLUMN IF NOT EXISTS imap_port INTEGER DEFAULT 993,
    ADD COLUMN IF NOT EXISTS use_tls BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS use_ssl BOOLEAN DEFAULT TRUE;

-- Add OAuth fields (for Gmail/Outlook OAuth)
ALTER TABLE mailboxes
    ADD COLUMN IF NOT EXISTS access_token TEXT,
    ADD COLUMN IF NOT EXISTS refresh_token TEXT,
    ADD COLUMN IF NOT EXISTS token_expires_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS oauth_provider VARCHAR(50); -- 'gmail', 'outlook', 'none'

-- Add quota and rate limiting fields
ALTER TABLE mailboxes
    ADD COLUMN IF NOT EXISTS daily_limit INTEGER DEFAULT 50,
    ADD COLUMN IF NOT EXISTS daily_sent INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_reset_date DATE DEFAULT CURRENT_DATE;

-- Add metadata and tracking fields
ALTER TABLE mailboxes
    ADD COLUMN IF NOT EXISTS domain VARCHAR(255),
    ADD COLUMN IF NOT EXISTS rotation_group VARCHAR(100),
    ADD COLUMN IF NOT EXISTS reputation_score DECIMAL(5,2) DEFAULT 100.00,
    ADD COLUMN IF NOT EXISTS bounce_rate DECIMAL(5,2) DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS total_sent INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_replies INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_bounces INTEGER DEFAULT 0;

-- Update existing mailboxes with default SMTP/IMAP settings based on provider
UPDATE mailboxes 
SET 
    smtp_host = CASE 
        WHEN provider = 'gmail' THEN 'smtp.gmail.com'
        WHEN provider = 'outlook' THEN 'smtp-mail.outlook.com'
        ELSE 'smtp.gmail.com'
    END,
    smtp_port = CASE 
        WHEN provider = 'gmail' THEN 587
        WHEN provider = 'outlook' THEN 587
        ELSE 587
    END,
    imap_host = CASE 
        WHEN provider = 'gmail' THEN 'imap.gmail.com'
        WHEN provider = 'outlook' THEN 'outlook.office365.com'
        ELSE 'imap.gmail.com'
    END,
    imap_port = CASE 
        WHEN provider = 'gmail' THEN 993
        WHEN provider = 'outlook' THEN 993
        ELSE 993
    END,
    use_tls = TRUE,
    use_ssl = TRUE,
    oauth_provider = CASE 
        WHEN provider = 'gmail' THEN 'gmail'
        WHEN provider = 'outlook' THEN 'outlook'
        ELSE 'none'
    END
WHERE smtp_host IS NULL;

-- Extract domain from email if domain is null
UPDATE mailboxes 
SET domain = SUBSTRING(email_address FROM '@(.+)$')
WHERE domain IS NULL AND email_address LIKE '%@%';

-- Create campaign_mailbox relationship table (many-to-many)
CREATE TABLE IF NOT EXISTS campaign_mailboxes (
    id SERIAL PRIMARY KEY,
    campaign_id INTEGER NOT NULL,
    mailbox_id INTEGER NOT NULL REFERENCES mailboxes(id) ON DELETE CASCADE,
    priority INTEGER DEFAULT 0, -- Higher priority = used first
    rotation_weight INTEGER DEFAULT 1, -- Weight for round-robin rotation
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(campaign_id, mailbox_id)
);

CREATE INDEX idx_campaign_mailboxes_campaign ON campaign_mailboxes(campaign_id);
CREATE INDEX idx_campaign_mailboxes_mailbox ON campaign_mailboxes(mailbox_id);

-- Add comments for documentation
COMMENT ON TABLE mailboxes IS 'Email inboxes used to send campaigns. Supports SMTP/IMAP and OAuth authentication.';
COMMENT ON TABLE campaign_mailboxes IS 'Many-to-many relationship between campaigns and mailboxes for rotation and distribution.';
COMMENT ON COLUMN mailboxes.daily_limit IS 'Maximum emails that can be sent per day from this mailbox';
COMMENT ON COLUMN mailboxes.daily_sent IS 'Number of emails sent today (reset daily)';
COMMENT ON COLUMN mailboxes.reputation_score IS 'Deliverability reputation score (0-100)';
COMMENT ON COLUMN mailboxes.rotation_group IS 'Group identifier for mailbox rotation (e.g., "group1", "group2")';
COMMENT ON COLUMN campaign_mailboxes.priority IS 'Priority for mailbox selection (higher = used first)';
COMMENT ON COLUMN campaign_mailboxes.rotation_weight IS 'Weight for round-robin rotation (higher = more emails)';



