-- Core database schema for the AI B2B Cold Outreach Agent
-- Database: PostgreSQL

-- ================
-- USERS
-- ================
CREATE TABLE users (
    user_id         SERIAL PRIMARY KEY,
    full_name       VARCHAR(150) NOT NULL,
    email           VARCHAR(200) UNIQUE NOT NULL,
    password_hash   TEXT NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW()
);

-- ================
-- ICP PROFILES (Ideal Customer Profile)
-- What kind of company the user wants to target
-- ================
CREATE TABLE icp_profiles (
    icp_id              SERIAL PRIMARY KEY,
    user_id             INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    profile_name        VARCHAR(150) NOT NULL,        -- e.g. "Fintech CTO targets"
    target_industry     VARCHAR(150),
    target_titles       TEXT,                         -- e.g. "CTO, VP Engineering, Head of Ops"
    company_size_min    INT,
    company_size_max    INT,
    pain_points         TEXT,                         -- "struggling with SOC2 compliance"
    created_at          TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_icp_user ON icp_profiles(user_id);

-- ================
-- COMPANIES (Potential client companies found/matched to ICP)
-- ================
CREATE TABLE companies (
    company_id          SERIAL PRIMARY KEY,
    icp_id              INT REFERENCES icp_profiles(icp_id) ON DELETE SET NULL,
    name                VARCHAR(200) NOT NULL,
    website             VARCHAR(300),
    industry            VARCHAR(150),
    employee_count      INT,
    hq_location         VARCHAR(200),
    linkedin_url        VARCHAR(400),
    source              VARCHAR(100),                     -- "manual", "scraped", "api"
    enrichment_notes    TEXT,                             -- summary / notes for personalization
    lead_score          INT,                              -- 0-100 how good of a fit
    created_at          TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_companies_icp ON companies(icp_id);
CREATE INDEX idx_companies_score ON companies(lead_score);

-- ================
-- CONTACTS (Actual humans we will email)
-- ================
CREATE TABLE contacts (
    contact_id              SERIAL PRIMARY KEY,
    company_id              INT NOT NULL REFERENCES companies(company_id) ON DELETE CASCADE,
    first_name              VARCHAR(100),
    last_name               VARCHAR(100),
    job_title               VARCHAR(200),
    email                   VARCHAR(250),
    linkedin_url            VARCHAR(400),
    personalization_notes   TEXT,  -- ex: "Spoke at AWS Summit 2024", "posted about cost overruns"
    created_at              TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_contacts_company ON contacts(company_id);
CREATE INDEX idx_contacts_email ON contacts(email);

-- ================
-- CAMPAIGNS (Email outreach campaigns)
-- ================
CREATE TABLE campaigns (
    campaign_id         SERIAL PRIMARY KEY,
    user_id             INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    icp_id              INT REFERENCES icp_profiles(icp_id) ON DELETE SET NULL,
    campaign_name       VARCHAR(200) NOT NULL,        -- "Q4 Outreach to Robotics CTOs"
    status              VARCHAR(50) DEFAULT 'draft',  -- draft | running | paused | completed
    created_at          TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_campaigns_user ON campaigns(user_id);

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
-- Each row is an email that was or will be sent to a specific contact
-- ================
CREATE TABLE emails (
    email_id            SERIAL PRIMARY KEY,
    campaign_id         INT NOT NULL REFERENCES campaigns(campaign_id) ON DELETE CASCADE,
    contact_id          INT NOT NULL REFERENCES contacts(contact_id) ON DELETE CASCADE,
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
CREATE INDEX idx_emails_contact ON emails(contact_id);
CREATE INDEX idx_emails_status ON emails(status);
