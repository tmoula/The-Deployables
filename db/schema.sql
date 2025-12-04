-- Core database schema for the AI B2B Cold Outreach Agent
-- Database: PostgreSQL
-- Updated to reflect new relationship structure

-- ================
-- USERS
-- ================
CREATE TABLE users (
    id              SERIAL PRIMARY KEY,
    email           VARCHAR(200) UNIQUE NOT NULL,
    password_hash   TEXT NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW()
);

-- ================
-- COMPANY PROFILES (User's own company info)
-- Each user can save their company information
-- ================
CREATE TABLE company_profiles (
    id              SERIAL PRIMARY KEY,
    user_id         INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    website         VARCHAR(300),
    industry        VARCHAR(150),
    description     TEXT,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_company_profiles_user ON company_profiles(user_id);

-- ================
-- ICP PROFILES (Ideal Customer Profile)
-- Who they want to target - prospect client info
-- ================
CREATE TABLE icp_profiles (
    icp_id              SERIAL PRIMARY KEY,
    user_id             INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name                VARCHAR(150) NOT NULL,        -- e.g. "US SaaS Founders"
    target_industry     VARCHAR(150),
    target_titles       TEXT,                         -- e.g. "CTO, VP Engineering, Head of Ops"
    company_size_min    INT,
    company_size_max    INT,
    geo_region          VARCHAR(200),
    extra_notes         TEXT,
    created_at          TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_icp_user ON icp_profiles(user_id);

-- ================
-- LEAD BATCHES (Each "Generate" click)
-- Every time the user clicks Generate, you create a batch
-- ================
CREATE TABLE lead_batches (
    id              SERIAL PRIMARY KEY,
    user_id         INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    icp_id          INT REFERENCES icp_profiles(icp_id) ON DELETE SET NULL,
    source          VARCHAR(100) DEFAULT 'manual',    -- e.g. "linkedin", "apollo", "scraper"
    status          VARCHAR(50) DEFAULT 'pending',   -- "pending", "running", "ready", "failed"
    total_leads     INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_lead_batches_user ON lead_batches(user_id);
CREATE INDEX idx_lead_batches_icp ON lead_batches(icp_id);
CREATE INDEX idx_lead_batches_status ON lead_batches(status);

-- ================
-- LEADS (The actual people/companies you found)
-- All leads are tied to the user and the batch that created them
-- ================
CREATE TABLE leads (
    id              SERIAL PRIMARY KEY,
    batch_id        INT REFERENCES lead_batches(id) ON DELETE CASCADE,
    user_id         INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    first_name      VARCHAR(100),
    last_name       VARCHAR(100),
    company_name    VARCHAR(200),
    company_website VARCHAR(300),
    job_title       VARCHAR(200),
    email           VARCHAR(250),
    linkedin_url    VARCHAR(400),
    country         VARCHAR(100),
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_leads_batch ON leads(batch_id);
CREATE INDEX idx_leads_user ON leads(user_id);
CREATE INDEX idx_leads_email ON leads(email);
CREATE INDEX idx_leads_company ON leads(company_name);

-- ================
-- CAMPAIGNS (Email outreach campaigns)
-- In the simple version, 1 campaign uses one batch of leads
-- ================
CREATE TABLE campaigns (
    campaign_id         SERIAL PRIMARY KEY,
    user_id             INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name                VARCHAR(200) NOT NULL,        -- "Q4 Outreach to Robotics CTOs"
    from_mailbox_id     BIGINT REFERENCES mailboxes(id) ON DELETE SET NULL,
    lead_batch_id       INT REFERENCES lead_batches(id) ON DELETE SET NULL,
    status              VARCHAR(50) DEFAULT 'draft',  -- draft | running | paused | completed
    created_at          TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_campaigns_user ON campaigns(user_id);
CREATE INDEX idx_campaigns_lead_batch ON campaigns(lead_batch_id);

-- ================
-- MAILBOXES (Gmail account storage)
-- ================
CREATE TABLE mailboxes (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email               VARCHAR(255) NOT NULL,
    display_name        VARCHAR(255),
    status              VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    encrypted_password  TEXT NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mailboxes_user_id ON mailboxes(user_id);

-- ================
-- EMAIL SEQUENCES (step 1, step 2, step 3 follow-ups...)
-- ================
CREATE TABLE email_sequences (
    sequence_id             SERIAL PRIMARY KEY,
    campaign_id             INT NOT NULL REFERENCES campaigns(campaign_id) ON DELETE CASCADE,
    step_number             INT NOT NULL,                -- 1, 2, 3...
    delay_days_after_prev   INT DEFAULT 0,               -- wait time before sending this step
    subject_template        TEXT NOT NULL,
    body_template           TEXT NOT NULL,
    created_at              TIMESTAMP DEFAULT NOW(),
    UNIQUE (campaign_id, step_number)
);

-- ================
-- EMAILS (actual sends / tracking)
-- Each row is an email that was or will be sent to a specific lead
-- ================
CREATE TABLE emails (
    email_id            SERIAL PRIMARY KEY,
    campaign_id         INT NOT NULL REFERENCES campaigns(campaign_id) ON DELETE CASCADE,
    lead_id             INT REFERENCES leads(id) ON DELETE CASCADE,
    sequence_step       INT,                     -- which step_number this came from
    final_subject       TEXT NOT NULL,
    final_body          TEXT NOT NULL,
    scheduled_at        TIMESTAMP,
    sent_at             TIMESTAMP,
    opened_at           TIMESTAMP,
    replied_at          TIMESTAMP,
    status              VARCHAR(50) DEFAULT 'scheduled', -- scheduled|sent|bounced|opened|replied
    created_at          TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_emails_campaign ON emails(campaign_id);
CREATE INDEX idx_emails_lead ON emails(lead_id);
CREATE INDEX idx_emails_status ON emails(status);

-- ================
-- RELATIONSHIP SUMMARY:
-- ================
-- users (1) → (many) company_profiles
-- users (1) → (many) icp_profiles
-- users (1) → (many) lead_batches
-- users (1) → (many) leads
-- users (1) → (many) campaigns
-- icp_profiles (1) → (many) lead_batches
-- lead_batches (1) → (many) leads
-- lead_batches (1) → (many) campaigns (via lead_batch_id)
-- campaigns (1) → (many) email_sequences
-- campaigns (1) → (many) emails
-- leads (1) → (many) emails
