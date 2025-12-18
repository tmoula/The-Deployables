# Technical Verification Guide
## For Professors & Technical Reviewers

This guide provides a systematic approach to verify all project requirements, architectural decisions, and technical implementations.

---

## Table of Contents

1. [Twelve-Factor App Compliance](#twelve-factor-app-compliance)
2. [Architecture Verification](#architecture-verification)
3. [Database Schema Documentation](#database-schema-documentation)
4. [API Documentation](#api-documentation)
5. [Testing Requirements Verification](#testing-requirements-verification)
6. [Kubernetes Deployment Verification](#kubernetes-deployment-verification)
7. [RabbitMQ Integration Verification](#rabbitmq-integration-verification)
8. [Security Checklist](#security-checklist)
9. [Code Coverage Verification](#code-coverage-verification)

---

## Twelve-Factor App Compliance

### ✅ Factor I: Codebase
**Requirement**: Single codebase tracked in revision control, many deploys

**Verification Steps**:
1. ✅ Check repository structure:
   ```bash
   git log --oneline | head -20
   ls -la apps/ frontend/ infra/
   ```
2. ✅ Verify single repository contains all services:
   - `apps/auth-svc/` - Authentication service
   - `apps/campaign-svc/` - Campaign management service
   - `apps/lead-svc/` - Lead generation service
   - `apps/AI-svc/` - AI service (Python)
   - `frontend/` - React frontend
   - `infra/` - Infrastructure as code

**Evidence**: Single GitHub repository with all microservices

---

### ✅ Factor II: Dependencies
**Requirement**: Explicitly declare and isolate dependencies

**Verification Steps**:
1. ✅ Check dependency declarations:
   ```bash
   # Java services
   cat apps/auth-svc/build.gradle | grep -A 20 "dependencies"
   cat apps/campaign-svc/build.gradle | grep -A 20 "dependencies"
   cat apps/lead-svc/build.gradle | grep -A 20 "dependencies"
   
   # Python service
   cat apps/AI-svc/requirements.txt
   
   # Frontend
   cat frontend/package.json
   ```
2. ✅ Verify containerization isolates dependencies:
   ```bash
   ls apps/*/Dockerfile
   ls frontend/Dockerfile
   ```
3. ✅ Check `.gitignore` excludes build artifacts:
   ```bash
   cat .gitignore | grep -E "build|node_modules|target|\.jar"
   ```

**Evidence**: All dependencies declared in `build.gradle`, `requirements.txt`, `package.json`. Dockerfiles use multi-stage builds.

---

### ✅ Factor III: Config
**Requirement**: Store config in the environment

**Verification Steps**:
1. ✅ Check Kubernetes ConfigMaps:
   ```bash
   kubectl get configmaps -n deps-lead-svc
   kubectl describe configmap outreach-config -n deps-lead-svc
   ```
2. ✅ Verify no hardcoded values in code:
   ```bash
   grep -r "localhost:8081" apps/ --exclude-dir=build
   grep -r "jdbc:postgresql://localhost" apps/ --exclude-dir=build
   ```
3. ✅ Check environment variable usage:
   ```bash
   # In deployment manifests
   grep -r "env:" infra/k8s/*.yaml
   grep -r "valueFrom:" infra/k8s/*.yaml
   ```
4. ✅ Verify ConfigMap references:
   ```bash
   cat infra/k8s/configmap.yaml
   ```

**Evidence**: All configuration via environment variables, ConfigMaps, and Secrets. No hardcoded credentials.

---

### ✅ Factor IV: Backing Services
**Requirement**: Treat backing services as attached resources

**Verification Steps**:
1. ✅ Verify database as attached resource:
   ```bash
   kubectl get svc -n deps-lead-svc | grep postgres
   # Check connection strings use service names
   grep -r "SPRING_DATASOURCE_URL" infra/k8s/*.yaml
   ```
2. ✅ Verify RabbitMQ as attached resource:
   ```bash
   kubectl get svc -n deps-lead-svc | grep rabbitmq
   grep -r "RABBITMQ" infra/k8s/*.yaml
   ```
3. ✅ Check service discovery via DNS:
   ```bash
   # Services reference each other by Kubernetes DNS names
   grep -r "lead-svc:8084" infra/k8s/*.yaml
   grep -r "postgres-service" infra/k8s/*.yaml
   ```

**Evidence**: PostgreSQL and RabbitMQ configured as Kubernetes Services, accessed via DNS names.

---

### ✅ Factor V: Build, Release, Run
**Requirement**: Strictly separate build and run stages

**Verification Steps**:
1. ✅ Check CI/CD pipeline:
   ```bash
   cat .github/workflows/ci.yml
   cat .github/workflows/cd.yml
   ```
2. ✅ Verify Docker multi-stage builds:
   ```bash
   grep -A 5 "FROM.*as" apps/*/Dockerfile
   grep -A 5 "FROM.*as" frontend/Dockerfile
   ```
3. ✅ Check semantic versioning:
   ```bash
   git tag | grep -E "v[0-9]+\.[0-9]+\.[0-9]+"
   ```
4. ✅ Verify GitOps-style manifests:
   ```bash
   ls -la infra/k8s/*.yaml
   ```

**Evidence**: GitHub Actions CI/CD, multi-stage Dockerfiles, versioned container images in Harbor.

---

### ✅ Factor VI: Processes
**Requirement**: Execute the app as one or more stateless processes

**Verification Steps**:
1. ✅ Verify stateless services:
   ```bash
   # Check no local file storage
   grep -r "FileOutputStream\|FileWriter" apps/*/src --exclude-dir=test
   ```
2. ✅ Check horizontal scaling:
   ```bash
   grep -r "replicas:" infra/k8s/*-deploy-k8s.yaml
   ```
3. ✅ Verify shared state in database only:
   ```bash
   # All services use PostgreSQL for state
   grep -r "@Entity\|@Repository" apps/*/src/main/java
   ```

**Evidence**: Services are stateless, use PostgreSQL for shared state, support horizontal scaling via replicas.

---

### ✅ Factor VII: Port Binding
**Requirement**: Export services via port binding

**Verification Steps**:
1. ✅ Check container port binding:
   ```bash
   grep -r "containerPort:" infra/k8s/*-deploy-k8s.yaml
   ```
2. ✅ Verify Kubernetes Services:
   ```bash
   kubectl get svc -n deps-lead-svc
   ```
3. ✅ Check Ingress configuration:
   ```bash
   cat infra/k8s/ingress.yaml
   kubectl get ingress -n deps-lead-svc
   ```
4. ✅ Verify health check endpoints:
   ```bash
   grep -r "/actuator/health" infra/k8s/*-deploy-k8s.yaml
   ```

**Evidence**: All services bind to ports, exposed via Kubernetes Services and Ingress.

---

### ✅ Factor VIII: Concurrency
**Requirement**: Scale out via the process model

**Verification Steps**:
1. ✅ Check RabbitMQ workers:
   ```bash
   # Lead service publishes to RabbitMQ
   grep -r "RabbitMQClient\|@RabbitListener" apps/lead-svc/src
   
   # AI service consumes from RabbitMQ
   grep -r "pika\|RabbitMQ" apps/AI-svc/src
   ```
2. ✅ Verify horizontal pod autoscaling (if configured):
   ```bash
   kubectl get hpa -n deps-lead-svc
   ```
3. ✅ Check concurrent job processing:
   ```bash
   # RabbitMQ listeners process jobs concurrently
   grep -r "concurrent.*consumers\|prefetch" apps/*/src
   ```

**Evidence**: RabbitMQ workers for async processing, Kubernetes replicas for horizontal scaling.

---

### ✅ Factor IX: Disposability
**Requirement**: Maximize robustness with fast startup and graceful shutdown

**Verification Steps**:
1. ✅ Check health probes:
   ```bash
   grep -A 10 "livenessProbe:" infra/k8s/*-deploy-k8s.yaml
   grep -A 10 "readinessProbe:" infra/k8s/*-deploy-k8s.yaml
   ```
2. ✅ Verify graceful shutdown:
   ```bash
   # Spring Boot handles SIGTERM gracefully
   grep -r "spring.lifecycle.timeout-per-shutdown-phase" apps/*/src
   ```
3. ✅ Check startup time:
   ```bash
   kubectl get pods -n deps-lead-svc -w
   # Observe pod startup times
   ```

**Evidence**: Liveness and readiness probes configured, Spring Boot handles graceful shutdown.

---

### ✅ Factor X: Dev/Prod Parity
**Requirement**: Keep development, staging, and production as similar as possible

**Verification Steps**:
1. ✅ Compare local vs production:
   ```bash
   cat infra/docker-compose.yml | grep -E "image:|environment:"
   cat infra/k8s/*-deploy-k8s.yaml | grep -E "image:|env:"
   ```
2. ✅ Verify same database schema:
   ```bash
   # Schema used in both environments
   cat db/schema.sql | head -50
   ```

**Evidence**: Same container images, same database schema, environment differences via ConfigMaps.

---

### ✅ Factor XI: Logs
**Requirement**: Treat logs as event streams

**Verification Steps**:
1. ✅ Check structured logging:
   ```bash
   grep -r "Logger\|log\." apps/*/src/main/java | head -10
   ```
2. ✅ Verify correlation IDs (if implemented):
   ```bash
   grep -r "correlation\|traceId" apps/*/src
   ```
3. ✅ Check log aggregation:
   ```bash
   kubectl logs -n deps-lead-svc deployment/lead-deployment --tail=50
   ```

**Evidence**: Spring Boot logging, Kubernetes log aggregation, structured log output.

---

### ✅ Factor XII: Admin Processes
**Requirement**: Run admin/management tasks as one-off processes

**Verification Steps**:
1. ✅ Check database migrations:
   ```bash
   ls apps/auth-svc/src/main/resources/db/migration/
   ```
2. ✅ Verify one-off job containers (if any):
   ```bash
   kubectl get jobs -n deps-lead-svc
   ```

**Evidence**: Database migrations as one-off processes, Kubernetes Jobs for admin tasks.

---

## Architecture Verification

### Microservices Architecture

**Verification Steps**:

1. **Service Count & Responsibilities**:
   ```bash
   # Verify 4 microservices (3 Spring Boot + 1 Python)
   ls -d apps/*-svc apps/AI-svc
   
   # Check service responsibilities
   cat README.md | grep -A 5 "Service | Default Port"
   ```

2. **Service Communication**:
   ```bash
   # HTTP/REST communication
   grep -r "RestTemplate\|WebClient\|@FeignClient" apps/*/src
   
   # RabbitMQ async communication
   grep -r "RabbitTemplate\|@RabbitListener" apps/*/src
   ```

3. **Service Independence**:
   ```bash
   # Each service has its own database schema/tables
   cat db/schema.sql | grep -E "CREATE TABLE.*campaigns|CREATE TABLE.*users|CREATE TABLE.*prospects"
   ```

**Evidence**: 4 independent microservices, REST for sync, RabbitMQ for async, separate database tables.

---

### Data Flow Verification

**Verification Steps**:

1. **Lead Generation Flow**:
   - Frontend → `POST /api/v1/match` → lead-svc
   - lead-svc → Publishes to RabbitMQ `ai.leads.generation.requests`
   - AI Service → Consumes from RabbitMQ, processes, publishes to `ai.leads.generation.responses`
   - lead-svc → Listens on response queue, saves to PostgreSQL

2. **Campaign Creation Flow**:
   - Frontend → `POST /api/v1/campaigns/upload-csv` → campaign-svc
   - campaign-svc → Saves to PostgreSQL, calls lead-svc for contacts
   - Email sending → Queued in RabbitMQ (if implemented)

3. **Verify in Code**:
   ```bash
   # Lead generation RabbitMQ
   grep -r "generateMatchingCompanies\|ai.leads.generation" apps/lead-svc/src
   grep -r "ai.leads.generation" apps/AI-svc/src
   
   # Campaign creation
   grep -r "upload-csv\|createCampaign" apps/campaign-svc/src
   ```

---

## Database Schema Documentation

### Schema Location
- **Primary Schema**: `db/schema.sql`
- **Migrations**: `apps/auth-svc/src/main/resources/db/migration/`

### Verification Steps

1. **View Complete Schema**:
   ```bash
   cat db/schema.sql
   ```

2. **Key Tables**:
   - `users` - User accounts
   - `sender_companies` - Sender company profiles
   - `mailboxes` - Email accounts for sending
   - `icp_profiles` - Ideal Customer Profile definitions
   - `campaigns` - Campaign definitions
   - `leads` - Imported leads/contacts
   - `email_events` - Email sending events
   - `lead_batches` - Lead generation batches
   - `prospects` - Generated prospects

3. **ER Diagram**:
   - See README.md for database schema diagram
   - Verify relationships: Foreign keys, CASCADE deletes

4. **Database Access**:
   ```bash
   # Production access
   kubectl port-forward -n deps-lead-svc svc/postgres-service 5432:5432
   psql -h localhost -U outreach_user -d outreachdb
   ```

---

## API Documentation

### Swagger/OpenAPI Endpoints

**Verification Steps**:

1. **Campaign Service**:
   - URL: `http://outreach.javajon-gke.duckdns.org/api/v1/campaigns/swagger`
   - Or: `http://localhost:8081/swagger` (local)
   - Verify: OpenAPI spec, request/response schemas

2. **Lead Service**:
   - URL: `http://outreach.javajon-gke.duckdns.org/api/v1/swagger`
   - Or: `http://localhost:8084/swagger` (local)
   - Verify: Match endpoint, batch status endpoints

3. **Auth Service**:
   - Check if Swagger enabled:
   ```bash
   grep -r "springdoc\|swagger" apps/auth-svc/
   ```

4. **Verify OpenAPI Configuration**:
   ```bash
   grep -r "springdoc.openapi" apps/*/src/main/resources
   grep -r "@Operation\|@ApiResponse" apps/*/src/main/java | head -10
   ```

**Evidence**: Swagger UI accessible, OpenAPI specs generated, detailed request/response schemas.

---

## Testing Requirements Verification

### Test Coverage Requirements

**Target**: 80% code coverage across all backend services

**Verification Steps**:

1. **Check Test Structure**:
   ```bash
   find apps/*/src/test -name "*Test.java" | wc -l
   find apps/*/src/test -name "*Test.java"
   ```

2. **Run Tests with Coverage**:
   ```bash
   # For each Java service
   cd apps/lead-svc && ./gradlew test jacocoTestReport
   cd apps/campaign-svc && ./gradlew test jacocoTestReport
   cd apps/auth-svc && ./gradlew test jacocoTestReport
   
   # View coverage reports
   open apps/lead-svc/build/reports/jacoco/test/html/index.html
   ```

3. **Check Test Types**:
   ```bash
   # Unit tests
   find apps/*/src/test -name "*Test.java" -exec grep -l "@Test" {} \;
   
   # Integration tests
   find apps/*/src/test -name "*IntegrationTest.java\|*IT.java"
   
   # API tests
   find apps/*/src/test -name "*ControllerTest.java"
   ```

4. **Verify Test Coverage Files**:
   ```bash
   ls apps/*/build/reports/jacoco/test/html/index.html
   cat apps/lead-svc/TEST_COVERAGE_SUMMARY.md
   ```

### Test Categories

1. **Unit Tests**:
   - Service layer tests
   - Repository tests
   - Domain model tests
   - Location: `apps/*/src/test/java/**/*Test.java`

2. **Integration Tests**:
   - Service-to-service communication
   - Database integration
   - RabbitMQ integration
   - Location: `apps/*/src/test/java/**/*IntegrationTest.java`

3. **API Tests**:
   - Controller tests with MockMvc
   - Request/response validation
   - Error handling
   - Location: `apps/*/src/test/java/**/*ControllerTest.java`

4. **Frontend Tests**:
   ```bash
   cd frontend && npm test -- --coverage
   ```

5. **End-to-End Tests**:
   - Complete user workflow tests
   - See `TEST_LOCAL.md` for test scripts

**Evidence**: Test files exist, JaCoCo reports generated, coverage summaries documented.

---

## Kubernetes Deployment Verification

### Deployment Checklist

**Verification Steps**:

1. **Namespace Isolation**:
   ```bash
   kubectl get namespace deps-lead-svc
   kubectl get role,rolebinding -n deps-lead-svc
   ```

2. **Resource Limits & Requests**:
   ```bash
   grep -A 5 "resources:" infra/k8s/*-deploy-k8s.yaml
   ```

3. **Non-Root Container Users**:
   ```bash
   grep -r "runAsUser\|runAsNonRoot" infra/k8s/*-deploy-k8s.yaml
   grep -r "USER" apps/*/Dockerfile frontend/Dockerfile
   ```

4. **Health Probes**:
   ```bash
   grep -A 10 "livenessProbe:" infra/k8s/*-deploy-k8s.yaml
   grep -A 10 "readinessProbe:" infra/k8s/*-deploy-k8s.yaml
   ```

5. **ConfigMaps & Secrets**:
   ```bash
   kubectl get configmaps -n deps-lead-svc
   kubectl get secrets -n deps-lead-svc
   cat infra/k8s/configmap.yaml
   ```

6. **Services & Ingress**:
   ```bash
   kubectl get svc -n deps-lead-svc
   kubectl get ingress -n deps-lead-svc
   cat infra/k8s/ingress.yaml
   ```

7. **Horizontal Pod Autoscaler** (if configured):
   ```bash
   kubectl get hpa -n deps-lead-svc
   ```

8. **Multi-Stage Dockerfiles**:
   ```bash
   grep -A 5 "FROM.*as" apps/*/Dockerfile
   grep -A 5 "FROM.*as" frontend/Dockerfile
   ```

9. **Deployment Status**:
   ```bash
   kubectl get deployments -n deps-lead-svc
   kubectl get pods -n deps-lead-svc
   kubectl describe deployment lead-deployment -n deps-lead-svc
   ```

**Evidence**: All Kubernetes manifests in `infra/k8s/`, proper resource limits, health probes, ConfigMaps/Secrets.

---

## RabbitMQ Integration Verification

### Async Processing Requirements

**Verification Steps**:

1. **Message Publishing**:
   ```bash
   # Lead service publishes to RabbitMQ
   grep -r "rabbitTemplate.convertAndSend\|generateMatchingCompanies" apps/lead-svc/src
   
   # Check queue names
   grep -r "ai.leads.generation.requests\|ai.email.generation.requests" apps/*/src
   ```

2. **Message Consumption**:
   ```bash
   # AI service consumes from RabbitMQ
   grep -r "@RabbitListener\|pika" apps/AI-svc/src
   grep -r "RabbitMQResponseListener" apps/lead-svc/src
   ```

3. **Dead Letter Queue Handling**:
   ```bash
   grep -r "dead.*letter\|DLX\|x-dead-letter" apps/*/src infra/k8s/*.yaml
   ```

4. **Retry Logic**:
   ```bash
   grep -r "retry\|@Retryable\|maxAttempts" apps/*/src
   ```

5. **Job Status Persistence**:
   ```bash
   # Batch status tracking
   grep -r "LeadBatchEntity\|batch.*status" apps/lead-svc/src
   ```

6. **Queue Monitoring**:
   ```bash
   # Access RabbitMQ management UI
   kubectl port-forward -n deps-lead-svc svc/rabbitmq 15672:15672
   # Visit http://localhost:15672 (guest/guest)
   ```

7. **Verify Queue Configuration**:
   ```bash
   grep -r "rabbitmq.*queue" infra/k8s/*.yaml
   cat infra/k8s/rabbitmq-deploy-k8s.yaml
   ```

**Evidence**: RabbitMQ queues configured, message publishing/consumption implemented, batch tracking in database.

---

## Security Checklist

### Security Requirements

**Verification Steps**:

1. **Non-Root Containers**:
   ```bash
   grep -r "USER\|runAsNonRoot\|runAsUser" apps/*/Dockerfile infra/k8s/*-deploy-k8s.yaml
   ```

2. **Secrets Management**:
   ```bash
   kubectl get secrets -n deps-lead-svc
   cat infra/k8s/secrets.yaml.example
   # Verify no secrets in code
   grep -r "password.*=.*["\']" apps/*/src --exclude-dir=test
   ```

3. **RBAC Configuration**:
   ```bash
   kubectl get role,rolebinding,serviceaccount -n deps-lead-svc
   ```

4. **JWT Authentication**:
   ```bash
   grep -r "JWT\|jwt\|Token" apps/auth-svc/src
   ```

5. **Input Validation**:
   ```bash
   grep -r "@Valid\|@NotBlank\|@NotNull" apps/*/src/main/java
   ```

6. **CORS Configuration**:
   ```bash
   grep -r "CorsConfig\|@CrossOrigin" apps/*/src
   ```

7. **SQL Injection Prevention**:
   ```bash
   # Verify JPA usage (parameterized queries)
   grep -r "@Query\|findBy" apps/*/src/main/java | head -10
   ```

8. **HTTPS/TLS** (if configured):
   ```bash
   kubectl get ingress -n deps-lead-svc -o yaml | grep -i tls
   ```

**Evidence**: Non-root users, secrets in Kubernetes Secrets, RBAC configured, JWT auth, input validation.

---

## Code Coverage Verification

### Coverage Reports

**Verification Steps**:

1. **Generate Coverage Reports**:
   ```bash
   # Lead Service
   cd apps/lead-svc
   ./gradlew clean test jacocoTestReport
   cat build/reports/jacoco/test/html/index.html
   
   # Campaign Service
   cd apps/campaign-svc
   ./gradlew clean test jacocoTestReport
   
   # Auth Service
   cd apps/auth-svc
   ./gradlew clean test jacocoTestReport
   ```

2. **View Coverage Reports**:
   ```bash
   # Open HTML reports
   open apps/lead-svc/build/reports/jacoco/test/html/index.html
   open apps/campaign-svc/build/reports/jacoco/test/html/index.html
   open apps/auth-svc/build/reports/jacoco/test/html/index.html
   ```

3. **Check Coverage Summaries**:
   ```bash
   cat apps/lead-svc/TEST_COVERAGE_SUMMARY.md
   cat apps/lead-svc/UNIT_TESTS_SUMMARY.md
   ```

4. **Verify Coverage Threshold**:
   ```bash
   # Check JaCoCo configuration
   grep -A 10 "jacoco" apps/*/build.gradle
   ```

5. **Coverage by Package**:
   - Check coverage for:
     - API/Controller layer
     - Application/Service layer
     - Infrastructure layer
     - Domain layer

**Target**: 80% coverage across all backend services

**Evidence**: JaCoCo reports generated, coverage summaries documented, test files organized by layer.

---

## Quick Verification Commands

### One-Line Verification Script

```bash
#!/bin/bash
echo "=== Twelve-Factor App Verification ==="
echo "1. Codebase: $(git remote -v | head -1)"
echo "2. Dependencies: $(ls apps/*/build.gradle apps/AI-svc/requirements.txt frontend/package.json 2>/dev/null | wc -l) dependency files"
echo "3. Config: $(kubectl get configmaps -n deps-lead-svc 2>/dev/null | wc -l) ConfigMaps"
echo "4. Backing Services: $(kubectl get svc -n deps-lead-svc | grep -E 'postgres|rabbitmq' | wc -l) services"
echo "5. Build/Release: $(ls .github/workflows/*.yml 2>/dev/null | wc -l) CI/CD workflows"
echo "6. Processes: $(kubectl get deployments -n deps-lead-svc 2>/dev/null | wc -l) stateless deployments"
echo "7. Port Binding: $(kubectl get svc -n deps-lead-svc 2>/dev/null | wc -l) services"
echo "8. Concurrency: $(grep -r 'replicas:' infra/k8s/*-deploy-k8s.yaml 2>/dev/null | wc -l) scaled services"
echo "9. Disposability: $(grep -r 'livenessProbe:' infra/k8s/*-deploy-k8s.yaml 2>/dev/null | wc -l) health probes"
echo ""
echo "=== Architecture ==="
echo "Microservices: $(ls -d apps/*-svc apps/AI-svc 2>/dev/null | wc -l)"
echo "Database Tables: $(grep -c 'CREATE TABLE' db/schema.sql 2>/dev/null)"
echo ""
echo "=== Testing ==="
echo "Test Files: $(find apps/*/src/test -name '*Test.java' 2>/dev/null | wc -l)"
echo ""
echo "=== Kubernetes ==="
echo "Deployments: $(kubectl get deployments -n deps-lead-svc 2>/dev/null | wc -l)"
echo "Pods: $(kubectl get pods -n deps-lead-svc 2>/dev/null | wc -l)"
echo "Services: $(kubectl get svc -n deps-lead-svc 2>/dev/null | wc -l)"
```

---

## Production Access Information

- **Production URL**: https://outreach.javajon-gke.duckdns.org
- **Namespace**: `deps-lead-svc`
- **Registry**: `harbor.javajon-gke.duckdns.org`
- **Database**: PostgreSQL in Kubernetes
- **Message Queue**: RabbitMQ in Kubernetes

### Access Commands

```bash
# Connect to cluster
gcloud container clusters get-credentials <cluster-name> --zone <zone>

# View all resources
kubectl get all -n deps-lead-svc

# Access services
kubectl port-forward -n deps-lead-svc svc/postgres-service 5432:5432
kubectl port-forward -n deps-lead-svc svc/rabbitmq 15672:15672

# View logs
kubectl logs -n deps-lead-svc deployment/lead-deployment --tail=100
```

---

## Additional Resources

- **Architecture Diagram**: See README.md
- **Database Schema**: `db/schema.sql`
- **Local Testing**: `TEST_LOCAL.md`
- **CI/CD Testing**: `TEST_CI_CD.md`
- **Service READMEs**: `apps/*/README.md`

---

## Verification Checklist Summary

- [ ] All 12 Twelve-Factor App principles verified
- [ ] Microservices architecture confirmed (4 services)
- [ ] Database schema documented and accessible
- [ ] API documentation (Swagger/OpenAPI) accessible
- [ ] Test coverage reports generated (target: 80%)
- [ ] Kubernetes deployment verified (manifests, health probes, resources)
- [ ] RabbitMQ integration verified (queues, publishing, consumption)
- [ ] Security checklist completed (non-root, secrets, RBAC, JWT)
- [ ] Code coverage verified (JaCoCo reports)

---

**Last Updated**: December 2024  
**Project**: The Deployables - AI B2B Cold Outreach Platform  
**Team**: Taha Moula, Daniel Simon, Edouard Carpe

