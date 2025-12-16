# The Deployables – AI B2B Cold Outreach Platform

An event-driven, microservices-based outreach platform that discovers prospects, generates personalized sequences with LLMs, and schedules multi-step email campaigns. Runs locally via Docker Compose and in production on GKE.

## PlantUML Architecture
<img width="1636" height="1075" alt="architecture diagram" src="https://github.com/user-attachments/assets/51384b85-eac2-4fa9-ab4f-017f9adc93ae" />

## Database Schema
<!-- Insert database picture here -->

---

## System At A Glance

| Service | Default Port | What it does | Data store | RabbitMQ queues |
|---------|--------------|--------------|------------|-----------------|
| Frontend (React) | 5173 (local) / 80 (prod) | UI for auth, campaigns, mailboxes, inbox | Browser localStorage for JWT/user info | n/a |
| Auth Service (Spring Boot) | 8083 | Register/login (JWT), mailbox CRUD, inbox fetch | PostgreSQL (`users`, `mailboxes`, `emails`) | n/a |
| Campaign Service (Spring Boot) | 8081 (`application.properties`) / docs say 8082 | CSV ingestion, campaign CRUD, email templates, scheduling, stats | PostgreSQL (`campaigns`, `leads`, `mailboxes`, `email_events`, etc.) | n/a |
| Lead Service (Spring Boot) | 8084 | Prospect/lead discovery, enrichment, AI personalization hooks, public API bridge | PostgreSQL (`prospects`, `seller_profiles`, batches) | `ai.leads.generation.requests` (publish) / `ai.leads.generation.responses` (listen) |
| AI Service (Python) | 8090 | LLM-based email generation adapter | Optional PostgreSQL context | `ai.email.generation.requests` (listen) / `ai.email.generation.responses` (publish) |
| Infrastructure | RabbitMQ 5672/15672, Postgres 5432, Harbor registry, GKE | Shared messaging + storage | Persistent volumes | n/a |

**PostgreSQL** is the primary database for all Java services. **RabbitMQ** is used for AI/lead generation workflows. Containers are deployed to GKE using manifests in `infra/k8s/`.

---

## End-to-End User Guide (What to click)

### 1) Start the stack locally
```bash
cd infra
docker-compose up --build -d
```
- Frontend: http://localhost:5173  
- Auth API: http://localhost:8083/api/v1/auth  
- Campaign API: http://localhost:8081/api/v1 (frontend defaults here)  
- Lead API: http://localhost:8084/api/v1  
- RabbitMQ UI: http://localhost:15672 (guest/guest)  
- Postgres: localhost:5432 (`postgres` / `postgres`, db `outreachdb`)

### 2) Register & log in (Frontend)
1. Go to **Register** → create an account (email/password).  
2. **Login** → token is stored in localStorage (`auth_token`) and used by `PrivateRoute` to gate the app.  
3. If auth API is unreachable, the UI will warn (see `authService.js`).

### 3) Connect a mailbox (Auth Service)
1. Navigate to **Mailboxes** (or Settings).  
2. Click **Add mailbox**, supply Gmail address + app password (`/api/v1/auth/mailboxes`).  
3. Status changes (pause/resume) and connectivity tests hit `/api/v1/auth/mailboxes/{id}/status` and `/test`.  
4. Inbox view uses `/api/v1/auth/inbox/all` and expects `X-User-Id` (frontend saves `userId` from login).

### 4) Generate Leads (Lead Service UI)
1. Go to **Leads** tab.
2. Fill out **Company Profile** (your details) and **Prospect Criteria** (who you want to target).
3. Click **Generate Leads** to use AI to find matching companies.
4. Once leads appear, click **Download CSV**.
5. You will use this CSV in the next step to create a campaign.

### 5) Create & send a campaign (Campaign Service UI)
Open **Campaigns** and click **+ Create Campaign**. The UI is a 5-step wizard:
1. **Upload CSV (Step 1)**  
   - Required: campaign name + CSV. **Upload the CSV you downloaded from the Leads page.**
   - Calls `POST /campaigns/upload-csv` (multipart). Creates campaign + imports leads to Postgres.  
2. **Compose Email (Step 2)**  
   - Write subject/body, manage variants and follow-ups.  
   - Merge fields come from detected CSV columns (shown after upload).  
3. **Campaign Settings (Step 3)**  
   - Pick timezone, daily send window, delay between emails, and mailboxes to use.  
4. **Review (Step 4)**  
   - Preview personalized emails per contact via `/campaigns/{id}/preview-email` (spintax + contact data).  
5. **Schedule & Send (Step 5)**  
   - Schedules via `/campaigns/{id}/schedule` with UTC conversion of your EST/EDT inputs.  
   - Stats and status polling come from `/campaigns/{id}/stats`.

While a campaign runs, the list view shows live progress (sent/queued/failed) and lets you start/pause/delete.

### 6) Leads & Inbox views
- **Leads tab** lists imported leads (data from Campaign/Lead services).  
- **Master Inbox** pulls recent emails from Auth Service’s EmailService (requires mailbox + user id).  
- **Dashboard** provides the guided tour and entry points to other modules.

---

## What Uses RabbitMQ vs PostgreSQL

**RabbitMQ**
- `ai.email.generation.requests` / `responses` (AI Service): queue-based email generation for campaigns.  
- `ai.leads.generation.requests` / `responses` (Lead Service): publishes prospect discovery tasks for async enrichment.  
- Health dashboards at http://localhost:15672 (guest/guest).

**PostgreSQL**
- **Auth**: users, JWT metadata, mailboxes, fetched emails.  
- **Campaign**: campaigns, steps, leads, stats, mailboxes, email events.  
- **Lead**: prospects, criteria, batches, seller profiles, generated hooks; also stores AI-generated company lists.  
- **AI**: may query the shared DB for campaign/contact context (see `apps/AI-svc/src/services/database_service.py`).  
- Schema source of truth: `db/schema.sql` (compose seeds the DB).

---

## Service Deep Dive

### Frontend (`frontend/`)
- React + Tailwind, routes guarded by `PrivateRoute`.  
- Uses `REACT_APP_AUTH_URL` (default `http://localhost:8083/api/v1/auth`) and `REACT_APP_API_URL` (default `http://localhost:8081/api/v1`).  
- Campaign wizard components: `Step1UploadCsv`, `Step2ComposeEmail`, `Step3CampaignSettings`, `Step4Review`, `Step5Send`.  
- Stores auth token/email/user id in `localStorage`; passes `X-User-Email` to Campaign API and `X-User-Id` to Auth inbox/mailbox endpoints.

### Auth Service (`apps/auth-svc/`, port 8083)
- Endpoints: `/api/v1/auth/register`, `/login`, `/verify`, `/validate`, `/mailboxes`, `/inbox/all`.  
- JWT auth (secret `JWT_SECRET`), SMTP settings in `application.properties`.  
- Persists users/mailboxes/emails in Postgres. No RabbitMQ dependency.

### Campaign Service (`apps/campaign-svc/`, port 8081 in config, README mentions 8082)
- Responsibilities: CSV ingestion, campaign CRUD, email template storage, preview, scheduling, stats.  
- Integrates with Lead Service (`LEAD_SERVICE_URL` env) to fetch contacts and with Auth mailboxes for sending.  
- Uses Postgres repositories for campaigns, leads, mailboxes, email events; no RabbitMQ usage.  
- Swagger (when enabled): `/swagger`.

### Lead Service (`apps/lead-svc/`, port 8084)
- Prospect discovery, matching, enrichment, personalization hook generation.  
- Calls AI Service over HTTP for company discovery and hooks; also publishes async discovery to RabbitMQ (`ai.leads.generation.requests`) and listens for responses.  
- Public API integration (toggle via `public.api.*` props).  
- Stores prospects, criteria, batches, seller profiles in Postgres.  
- Swagger: `/swagger`.

### AI Service (`apps/AI-svc/`, port 8090)
- RabbitMQ adapter: listens on `ai.email.generation.requests`, publishes to `ai.email.generation.responses`, errors to `.errors`.  
- Uses OpenAI primary, Gemini fallback. Optional Postgres context (`DB_*` env).  
- Minimal REST `/health` for liveness.

---

## Local Development (Docker Compose)

Prereqs: Docker/Compose, Java 17+, Node 18+. From `infra/`:
```bash
docker-compose up --build -d
```
Access:
- Frontend http://localhost:5173
- RabbitMQ UI http://localhost:15672 (guest/guest)
- Postgres localhost:5432 (`postgres`/`postgres`, db `outreachdb`)

See [TEST_LOCAL.md](TEST_LOCAL.md) for running unit tests across services.

---

## GKE Deployment (Production)
- Namespace: `deps-lead-svc`  
- Public URL: https://javajon-gke.duckdns.org  
- Registry: `harbor.javajon-gke.duckdns.org`  

CI/CD (GitHub Actions):
1. Build & push containers to Harbor on `main`.  
2. Deploy Kubernetes manifests in `infra/k8s/` to GKE.  

---

## Database Management
- Schema: `db/schema.sql` (apply with `psql -h localhost -U postgres -d outreachdb -f db/schema.sql`).  
- Local compose exposes Postgres on 5432.  
- Prod access: `kubectl port-forward -n deps-lead-svc svc/postgres-service 5432:5432`, then `psql`.  
- Credentials live in `infra/k8s/secrets.yaml` (example file) or the `outreach-secrets` secret.

---

## Known Issues / Troubles We’re Having
- **Queue name mismatch (AI vs Lead)**: Lead Service publishes to `ai.leads.generation.requests` while AI Service listens on `ai.email.generation.requests`; result: lead-enrichment jobs sit in a queue the AI adapter never reads, so company discovery/personalization via AI will stall. **Fix:** align both services to the same queue name (recommended: point Lead to `ai.email.generation.requests`) or add a consumer/alias for `ai.leads.generation.requests`.  
- **Port confusion (Campaign Service)**: Code/config use port 8081, but `apps/campaign-svc/README.md` claims 8082 and `lead.service.url` default points to `lead-svc:8081`. Verify and standardize ports before deployment.  
- **Auth mailbox/inbox relies on `X-User-Id`**: Without a gateway that injects user id from JWT, mailbox/inbox calls need the header; ensure frontend login stores `userId` and sends it.  
- **Partial backend wiring in UI**: Frontend assumes campaign/lead/auth services are reachable; if not, the campaign wizard will fail at upload/preview/schedule and inbox will be empty.  
- **Secrets & SMTP**: Mail sending requires valid Gmail app password (`MAIL_USERNAME`/`MAIL_PASSWORD`) in secrets; missing or wrong values break mailbox tests.  
- **OpenAI/Gemini keys required**: AI service needs `OPENAI_API_KEY` (and optional `GEMINI_API_KEY`) set in env/secrets; otherwise generation falls back or fails.

---

## Team
- Taha Moula  
- Daniel Simon  
- Edouard Carpe  

---

## AI Citations
- **Gemini 3 Pro**: Primary language model for email generation and content creation

