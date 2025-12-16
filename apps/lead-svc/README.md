## Lead Service

The lead service is a Java Spring Boot microservice responsible for managing prospects and lead data within The Deployables B2B outreach platform. It provides APIs to create, enrich, query, and maintain leads that are later used by the campaign service and AI email generation workflows.

---

## Responsibilities

The lead service is designed to:

- Store and manage companies, contacts, and related metadata.
- Support ingestion of leads from external sources or CSV imports (where implemented).
- Provide query and filtering capabilities for building target lists and Ideal Customer Profiles (ICPs).
- Integrate with the AI service to request enrichment or personalization data via RabbitMQ.
- Expose REST APIs consumed by the frontend and other microservices.

---

## Running the Service

From the `lead-svc` directory:

### Development

```bash
./gradlew bootRun
```

### Build

```bash
./gradlew build
```

### Run the packaged JAR

```bash
java -jar build/libs/lead-svc-0.0.1-SNAPSHOT.jar
```

---

## Configuration

Core configuration values are typically supplied via `application.yml`/`application.properties` or environment variables, including:

- PostgreSQL connection details (host, port, database, username, password).
- RabbitMQ connection details.
- Any feature flags or integration endpoints shared with other services.

In containerized and Kubernetes deployments, these values are provided by ConfigMaps and Secrets as documented in the root project and `infra/k8s` READMEs.

---

## Ports and Endpoints

- **Default HTTP port**: `8084`

API documentation (for example, via Springdoc/OpenAPI or Swagger UI) can be exposed and accessed locally. Check the service configuration for the exact path (commonly `/swagger-ui` or `/swagger-ui/index.html`).

---

## Relationship to Other Services

- The **campaign service** uses the lead service to retrieve target leads for specific campaigns.
- The **AI service** can be invoked (indirectly via RabbitMQ) to enrich or personalize lead data.
- The **frontend** consumes lead APIs to display lists, detail views, and ICP‑driven filters.

