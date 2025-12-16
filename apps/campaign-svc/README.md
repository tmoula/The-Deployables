## Campaign Service

The campaign service is a Java Spring Boot microservice responsible for managing outreach campaigns and orchestrating email sends within The Deployables platform. It coordinates campaign definitions, templates, personalization rules, and integration with downstream services such as the lead service, email providers, and the AI adapter.

---

## Responsibilities

The campaign service is designed to:

- Create, update, and archive outreach campaigns and their steps.
- Manage email templates and spintax rules for personalization.
- Schedule and execute campaign sends, including follow‑up sequences.
- Integrate with the **lead service** to fetch and associate prospects.
- Queue outbound email jobs and AI personalization requests via RabbitMQ.
- Track campaign performance and delivery results (where implemented).

---

## Running the Service

### Development

From the `campaign-svc` directory:

```bash
./gradlew bootRun
```

### Build

```bash
./gradlew build
```

### Run the packaged JAR

```bash
java -jar build/libs/campaign-svc-0.0.1-SNAPSHOT.jar
```

---

## Service Configuration

Key configuration is typically provided through environment variables or `application.yml`/`application.properties`, including:

- Database connection (PostgreSQL).
- RabbitMQ host, port, and credentials.
- Integration URLs or credentials for email providers and other internal services.

In containerized and Kubernetes environments, these values are sourced from ConfigMaps and Secrets as documented in the root and `infra/k8s` READMEs.

---

## Ports and Endpoints

- **Default HTTP port**: `8082`

### API Documentation

When running locally, OpenAPI/Swagger UI is available at:

```text
http://localhost:8082/swagger
```

Depending on the final configuration, the exact Swagger path may differ (for example, `/swagger-ui` or `/swagger-ui/index.html`). Consult the Spring configuration for the most accurate path.

