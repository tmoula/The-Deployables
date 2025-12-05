-- Clean Production Schema Migration
-- This replaces all previous database structure with a clean, production-ready schema

-- ============================================
-- STEP 1: Drop ALL existing tables (clean slate)
-- ============================================
DROP TABLE IF EXISTS email_events CASCADE;
DROP TABLE IF EXISTS sent_emails CASCADE;
DROP TABLE IF EXISTS campaign_leads CASCADE;
DROP TABLE IF EXISTS campaign_steps CASCADE;
DROP TABLE IF EXISTS campaigns CASCADE;
DROP TABLE IF EXISTS leads CASCADE;
DROP TABLE IF EXISTS lead_batches CASCADE;
DROP TABLE IF EXISTS icp_profiles CASCADE;
DROP TABLE IF EXISTS mailboxes CASCADE;
DROP TABLE IF EXISTS sender_companies CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS companies CASCADE;
DROP TABLE IF EXISTS contacts CASCADE;
DROP TABLE IF EXISTS company_profiles CASCADE;

-- ============================================
-- STEP 2: Create Core Tables
-- ============================================

-- 1. USERS - One per account
-- 1. USERS - One per account
CREATE TABLE users (
    id              SERIAL PRIMARY KEY,
    email           VARCHAR(200) UNIQUE NOT NULL,
    password_hash   TEXT NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    enabled         BOOLEAN DEFAULT TRUE,
    verification_code VARCHAR(10),
    is_verified     BOOLEAN DEFAULT FALSE,
    plan            VARCHAR(50) DEFAULT 'free',  -- free, pro, enterprise
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);

-- 2. SENDER_COMPANIES - User's own company (who they're sending from)
CREATE TABLE sender_companies (
    id              SERIAL PRIMARY KEY,
    user_id         INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    website         VARCHAR(300),
    industry        VARCHAR(150),
    description     TEXT,
    logo_url        VARCHAR(500),
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_sender_companies_user ON sender_companies(user_id);

-- 3. MAILBOXES - Email inboxes used to send campaigns
CREATE TABLE mailboxes (
    id              SERIAL PRIMARY KEY,
    user_id         INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email_address   VARCHAR(255) NOT NULL,
    provider        VARCHAR(50) NOT NULL,  -- gmail, outlook, smtp
    display_name    VARCHAR(255),
    is_verified     BOOLEAN DEFAULT FALSE,
    warmup_status   VARCHAR(50) DEFAULT 'none',  -- none, running, paused
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_mailboxes_user ON mailboxes(user_id);
CREATE INDEX idx_mailboxes_email ON mailboxes(email_address);

-- ============================================
-- STEP 3: Targeting - ICP Profiles
-- ============================================

-- 4. ICP_PROFILES - Who they want to target (prospect "persona")
CREATE TABLE icp_profiles (
    id              SERIAL PRIMARY KEY,
    user_id         INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(150) NOT NULL,  -- e.g. "US B2B SaaS Founders"
    target_industry VARCHAR(150),
    target_titles   TEXT,  -- e.g. "CEO, Founder, VP Sales"
    company_size_min INT,
    company_size_max INT,
    geo_region      VARCHAR(200),
    pain_points     TEXT,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_icp_profiles_user ON icp_profiles(user_id);

-- ============================================
-- STEP 4: Lead Generation
-- ============================================

-- 5. LEAD_BATCHES - One row per Generate click
CREATE TABLE lead_batches (
    id                  SERIAL PRIMARY KEY,
    user_id             INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    icp_id              INT REFERENCES icp_profiles(id) ON DELETE SET NULL,
    source              VARCHAR(50) NOT NULL,  -- ai_scraper, uploaded_csv, manual
    status              VARCHAR(50) DEFAULT 'pending',  -- pending, running, ready, failed
    requested_lead_count INT,
    total_leads         INT DEFAULT 0,
    error_message       TEXT,
    created_at          TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_lead_batches_user ON lead_batches(user_id);
CREATE INDEX idx_lead_batches_icp ON lead_batches(icp_id);
CREATE INDEX idx_lead_batches_status ON lead_batches(status);

-- 6. LEADS - Actual prospects found (or imported from CSV)
CREATE TABLE leads (
    id                  SERIAL PRIMARY KEY,
    user_id             INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    batch_id            INT REFERENCES lead_batches(id) ON DELETE SET NULL,
    first_name          VARCHAR(100),
    last_name           VARCHAR(100),
    job_title           VARCHAR(200),
    company_name        VARCHAR(200),
    company_website     VARCHAR(300),
    company_linkedin    VARCHAR(400),
    linkedin_url        VARCHAR(400),
    email               VARCHAR(250),
    country             VARCHAR(100),
    custom_notes        TEXT,  -- for personalization
    created_at          TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_leads_user ON leads(user_id);
CREATE INDEX idx_leads_batch ON leads(batch_id);
CREATE INDEX idx_leads_email ON leads(email);
CREATE INDEX idx_leads_company ON leads(company_name);

-- ============================================
-- STEP 5: Campaigns & Sequences
-- ============================================

-- 7. CAMPAIGNS - Top-level campaign
CREATE TABLE campaigns (
    id              SERIAL PRIMARY KEY,
    user_id         INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    icp_id          INT REFERENCES icp_profiles(id) ON DELETE SET NULL,
    lead_batch_id   INT REFERENCES lead_batches(id) ON DELETE SET NULL,
    mailbox_id      INT REFERENCES mailboxes(id) ON DELETE SET NULL,
    status          VARCHAR(50) DEFAULT 'draft',  -- draft, scheduled, running, paused, completed
    start_at        TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_campaigns_user ON campaigns(user_id);
CREATE INDEX idx_campaigns_icp ON campaigns(icp_id);
CREATE INDEX idx_campaigns_lead_batch ON campaigns(lead_batch_id);
CREATE INDEX idx_campaigns_mailbox ON campaigns(mailbox_id);
CREATE INDEX idx_campaigns_status ON campaigns(status);

-- 8. CAMPAIGN_STEPS - The sequence: Step 1, follow-up 1, follow-up 2, etc.
CREATE TABLE campaign_steps (
    id              SERIAL PRIMARY KEY,
    campaign_id     INT NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    step_order      INT NOT NULL,  -- 1, 2, 3...
    delay_hours     INT DEFAULT 0,  -- send this X hours after previous
    subject_template TEXT NOT NULL,
    body_template   TEXT NOT NULL,  -- with variables like {{first_name}}, {{company_name}}
    created_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE (campaign_id, step_order)
);

CREATE INDEX idx_campaign_steps_campaign ON campaign_steps(campaign_id);

-- 9. CAMPAIGN_LEADS - Many-to-many: attach specific leads to campaigns (optional but powerful)
CREATE TABLE campaign_leads (
    id              SERIAL PRIMARY KEY,
    campaign_id     INT NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    lead_id         INT NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    status          VARCHAR(50) DEFAULT 'queued',  -- queued, in_progress, completed, unsubscribed, bounced
    created_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE (campaign_id, lead_id)
);

CREATE INDEX idx_campaign_leads_campaign ON campaign_leads(campaign_id);
CREATE INDEX idx_campaign_leads_lead ON campaign_leads(lead_id);
CREATE INDEX idx_campaign_leads_status ON campaign_leads(status);

-- ============================================
-- STEP 6: Sending & Tracking
-- ============================================

-- 10. SENT_EMAILS - Each actual email that got queued/sent
CREATE TABLE sent_emails (
    id                  SERIAL PRIMARY KEY,
    campaign_id         INT NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    campaign_step_id    INT REFERENCES campaign_steps(id) ON DELETE SET NULL,
    lead_id             INT NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    mailbox_id          INT REFERENCES mailboxes(id) ON DELETE SET NULL,
    to_email            VARCHAR(250) NOT NULL,
    subject             TEXT NOT NULL,
    body                TEXT NOT NULL,
    status              VARCHAR(50) DEFAULT 'queued',  -- queued, sent, failed
    provider_message_id VARCHAR(255),  -- for SendGrid/Gmail/etc
    sent_at             TIMESTAMP,
    created_at          TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_sent_emails_campaign ON sent_emails(campaign_id);
CREATE INDEX idx_sent_emails_campaign_step ON sent_emails(campaign_step_id);
CREATE INDEX idx_sent_emails_lead ON sent_emails(lead_id);
CREATE INDEX idx_sent_emails_mailbox ON sent_emails(mailbox_id);
CREATE INDEX idx_sent_emails_status ON sent_emails(status);
CREATE INDEX idx_sent_emails_to_email ON sent_emails(to_email);

-- 11. EMAIL_EVENTS - Opens, clicks, replies, bounces
CREATE TABLE email_events (
    id              SERIAL PRIMARY KEY,
    sent_email_id   INT NOT NULL REFERENCES sent_emails(id) ON DELETE CASCADE,
    event_type      VARCHAR(50) NOT NULL,  -- opened, clicked, replied, bounced, unsubscribed
    event_at        TIMESTAMP DEFAULT NOW(),
    meta            JSONB  -- ip, user_agent, link_url, etc.
);

CREATE INDEX idx_email_events_sent_email ON email_events(sent_email_id);
CREATE INDEX idx_email_events_type ON email_events(event_type);
CREATE INDEX idx_email_events_at ON email_events(event_at);

-- ============================================
-- Migration Complete
-- ============================================
-- Final structure:
-- 1. users
-- 2. sender_companies
-- 3. mailboxes
-- 4. icp_profiles
-- 5. lead_batches
-- 6. leads
-- 7. campaigns
-- 8. campaign_steps
-- 9. campaign_leads (optional many-to-many)
-- 10. sent_emails
-- 11. email_events

