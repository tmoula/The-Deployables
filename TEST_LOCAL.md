# Local Testing Guide

This guide explains how to test your services locally, simulating the GKE environment using Docker Compose.

## 1. Automated Unit Tests

The simplest way to test your code is running the standard unit tests. These tests typically use an in-memory database (H2) and do not require external infrastructure.

```bash
# Run tests for a specific service (e.g., campaign-svc)
cd apps/campaign-svc
./gradlew test
```

## 2. Local Integration Environment (Docker Compose)

To test how your service interacts with the database, RabbitMQ, and other services (like `lead-svc`), you can use Docker Compose to spin up these dependencies locally.

### Start Dependencies
We have a helper script `infra/docker-compose.yml` that defines the environment.

```bash
cd infra
# Start Postgres, RabbitMQ, and dependent services (e.g. lead-svc, ai-svc)
docker-compose up -d postgres rabbitmq lead-svc ai-svc
```

### Check Running Services
Ensure everything is up:
```bash
docker-compose ps
```

## 3. Running Your Service Locally

Once dependencies are running, you can run your service locally and connect it to them. You need to override the default GKE URLs with localhost ports.

**Port Mappings (from docker-compose):**
- **Postgres**: `localhost:5432`
- **RabbitMQ**: `localhost:5672`
- **Lead Service**: `localhost:8084` (internally 8081)
- **AI Service**: `localhost:8090`

### Example: Running `campaign-svc`
You can run the service using `./gradlew bootRun` with environment variables:

```bash
cd apps/campaign-svc

# Set environment variables to point to local Docker containers
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/outreachdb
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export RABBITMQ_HOST=localhost
export RABBITMQ_PORT=5672
export LEAD_SERVICE_URL=http://localhost:8084

# Run the application
./gradlew bootRun
```

Now your local `campaign-svc` is running on port **8081** and connected to the local "cluster"!

## 4. Helper Script (`test_local.sh`)

We have created a script `test_local.sh` in the root directory to automate checking requirements and running tests.

```bash
# Verify unit tests
./test_local.sh unit

# Verify infrastructure connectivity (checks if dependencies are reachable)
./test_local.sh infra
```
