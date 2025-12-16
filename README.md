# The Deployables - AI B2B Cold Outreach Platform

An AI-powered microservices platform for B2B cold email outreach campaigns, featuring automated lead generation, intelligent email composition, and comprehensive campaign management.



## 🏗 System Architecture

The system is composed of event-driven microservices running on Kubernetes (GKE).

### Core Services
| Service | Port | Description |
|---------|------|-------------|
| **Frontend** | `5173` (Local) / `80` (Prod) | React + Vite UI for campaign management |
| **Auth Service** | `8083` | JWT authentication & User management |
| **Campaign Service** | `8081` | Campaign orchestration & sequencing |
| **Lead Service** | `8084` | Lead ingestion & management |
| **AI Service** | `8090` | Generates email copy using LLMs (OpenAI/Gemini) |

### Infrastructure
- **Message Broker**: RabbitMQ (Async communication between services)
- **Database**: PostgreSQL (Persistent storage for all services)
- **Registry**: Harbor (Container image storage)
- **Platform**: Google Kubernetes Engine (GKE)

---

## 🛠 Local Development (Docker Compose)

Run the entire platform locally with a single command.

### Prerequisites
- Docker & Docker Compose
- Java 17+
- Node.js 18+

### Quick Start
1.  **Clone the repository**:
    ```bash
    git clone https://github.com/tmoula/The-Deployables.git
    cd The-Deployables/infra
    ```

2.  **Start Services**:
    ```bash
    docker-compose up --build -d
    ```

3.  **Access Components**:
    -   **Frontend**: [http://localhost:5173](http://localhost:5173)
    -   **RabbitMQ Dashboard**: [http://localhost:15672](http://localhost:15672) (guest/guest)
    -   **Database**: `localhost:5432` (postgres/postgres)

For detailed local testing instructions (including unit tests), see [TEST_LOCAL.md](TEST_LOCAL.md).

---

## ☁️ GKE Deployment (Production)

The production environment runs on GKE in the `deps-lead-svc` namespace.

### Access Info
-   **URL**: [https://javajon-gke.duckdns.org](https://javajon-gke.duckdns.org)
-   **Registry**: `harbor.javajon-gke.duckdns.org`

## 📦 Database Management

The project uses PostgreSQL 16. The schema is defined in `db/schema.sql`.

### Local Initialization
When running with Docker Compose, the database is available at `localhost:5432`.

```bash
# Apply schema to local DB
psql -h localhost -U postgres -d outreachdb -f db/schema.sql
```

### Production Access (GKE)
To manage the production database (e.g., to inspect data or apply schema updates), you must port-forward:

1.  **Establish Connection**:
    ```bash
    kubectl port-forward -n deps-lead-svc svc/postgres-service 5432:5432
    ```

2.  **Connect via PSQL**:
    ```bash
    # Open a new terminal
    psql -h localhost -U postgres -d outreachdb
    ```

3.  **Authentication**:
    -   **User**: `postgres`
    -   **Password**: See `infra/k8s/secrets.yaml` (Base64 decoded) or `outreach-secrets` in cluster.

### Debugging & Verification

If you need to inspect the live system:

1.  **Check Pod Status**:
    ```bash
    kubectl get pods -n deps-lead-svc
    ```

2.  **View Logs**:
    ```bash
    kubectl logs -l app=campaign-svc -n deps-lead-svc
    ```

3.  **Port Forwarding (Debug DB/RabbitMQ)**:
    ```bash
    # RabbitMQ
    kubectl port-forward -n deps-lead-svc svc/rabbitmq 15672:15672
    
    # Postgres
    kubectl port-forward -n deps-lead-svc svc/postgres-service 5432:5432
    ```

### Deployment Flow
We use GitHub Actions for CI/CD:
1.  **CI (Build & Push)**: Triggers on push to `main`. Builds containers and pushes to Harbor Project `deps` (or `library`).
2.  **CD (Deploy)**: Deploys the Kubernetes manifests from `infra/k8s/` to GKE.

For detailed deployment info and troubleshooting, see [TEST_CI_CD.md](TEST_CI_CD.md).

## 👥 Team
- **EC** - CI/CD & External API
- **TM** - Email Functionality
- **JJ** - Database & Infra
- **DS** - System Architecture



## AI Citations

This project leverages the following AI technologies:
- **OpenAI GPT-4o-mini**: Primary language model for email generation and content creation
- **Google Gemini 2.5 Flash**: Fallback language model for high availability