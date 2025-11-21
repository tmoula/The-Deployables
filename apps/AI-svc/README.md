# AI Email Generation Adapter Service

This service acts as a **RabbitMQ adapter** that watches for email generation requests from microservices and processes them using AI (OpenAI or Ollama). The service listens to RabbitMQ queues, matches requests with microservice requests, and publishes responses back to RabbitMQ.

## Architecture

- **RabbitMQ Adapter Pattern**: The service consumes messages from RabbitMQ queues instead of exposing REST endpoints
- **Message-Driven**: Listens to `ai.email.generation.requests` queue and publishes to `ai.email.generation.responses`
- **Database Integration**: Can query the separate database container for additional context
- **Persistent Storage**: Database is in a separate container with persistent volume storage

## Features

- **RabbitMQ Integration**: Consumes email generation requests from RabbitMQ queues
- **Request Matching**: Matches incoming requests with microservice requests using request_id
- **Personalized Email Generation**: Creates customized emails based on company and contact information
- **Email Sequences**: Generates complete follow-up sequences
- **Flexible LLM Support**: Works with OpenAI API or local Ollama instances
- **Database Access**: Can query database for company/contact/campaign information
- **Error Handling**: Publishes errors to dedicated error queue

## Setup

### Prerequisites

- Python 3.11+
- OpenAI API key OR Ollama running locally

### Installation

1. Install dependencies:
```bash
pip install -r requirements.txt
```

2. Configure environment variables:
```bash
cp .env.example .env
# Edit .env with your API key
```

### Environment Variables

**RabbitMQ Configuration:**
- `RABBITMQ_HOST`: RabbitMQ host (default: localhost)
- `RABBITMQ_PORT`: RabbitMQ port (default: 5672)
- `RABBITMQ_USER`: RabbitMQ username (default: guest)
- `RABBITMQ_PASSWORD`: RabbitMQ password (default: guest)
- `RABBITMQ_VHOST`: RabbitMQ virtual host (default: /)
- `AI_REQUEST_QUEUE`: Queue name for incoming requests (default: ai.email.generation.requests)
- `AI_RESPONSE_QUEUE`: Queue name for responses (default: ai.email.generation.responses)
- `AI_ERROR_QUEUE`: Queue name for errors (default: ai.email.generation.errors)

**AI/LLM Configuration:**
- `OPENAI_API_KEY`: Your OpenAI API key (required if not using Ollama)
- `OPENAI_MODEL`: Model to use (default: gpt-4o-mini)
- `USE_OLLAMA`: Set to "true" to use Ollama instead of OpenAI
- `OLLAMA_URL`: Ollama server URL (default: http://localhost:11434)
- `OLLAMA_MODEL`: Ollama model name (default: llama3.2)

**Database Configuration:**
- `DB_HOST`: Database host (default: localhost)
- `DB_PORT`: Database port (default: 5432)
- `DB_NAME`: Database name (default: outreachdb)
- `DB_USER`: Database user (default: postgres)
- `DB_PASSWORD`: Database password (default: postgres)

**Service Configuration:**
- `PORT`: Health check server port (default: 8090)

## Running the Service

### Development

```bash
python ai-svc/main.py
```

Or with uvicorn:
```bash
uvicorn ai-svc.main:app --host 0.0.0.0 --port 8090 --reload
```

### Docker

```bash
docker build -t ai-svc .
docker run -p 8090:8090 --env-file .env ai-svc
```

## RabbitMQ Message Format

### Request Message (Published to `ai.email.generation.requests`)

```json
{
  "request_id": "unique-request-id-123",
  "company": {
    "company_id": 1,
    "name": "Acme Corp",
    "industry": "Technology",
    "employee_count": 150,
    "tech_stack": ["Python", "AWS"],
    "location": "San Francisco, CA",
    "website": "https://acme.com",
    "enrichment_notes": "Fast-growing startup"
  },
  "contact": {
    "contact_id": 1,
    "first_name": "John",
    "last_name": "Doe",
    "job_title": "CTO",
    "email": "john@acme.com",
    "personalization_notes": "Recently posted about scaling challenges"
  },
  "requirements": {
    "campaign_name": "Q4 Outreach",
    "product_service_description": "AI-powered analytics platform",
    "value_proposition": "Reduce operational costs by 30%",
    "target_pain_points": ["high costs", "manual processes"],
    "tone": "professional",
    "email_length": "medium",
    "include_call_to_action": true,
    "custom_instructions": "Focus on ROI"
  },
  "sequence_step": 1,
  "previous_email_context": null
}
```

### Response Message (Published to `ai.email.generation.responses`)

```json
{
  "request_id": "unique-request-id-123",
  "success": true,
  "subject": "Quick question about reducing operational costs",
  "body": "Hi John,\n\nI noticed Acme Corp is growing rapidly...",
  "personalization_score": 0.85,
  "generated_at": "2024-01-15T10:30:00"
}
```

### Error Message (Published to `ai.email.generation.errors`)

```json
{
  "request_id": "unique-request-id-123",
  "success": false,
  "error": "Error message here",
  "generated_at": "2024-01-15T10:30:00"
}
```

## Health Check Endpoint

The service exposes a minimal REST API for health checks:

```
GET /health
```

Returns:
```json
{
  "status": "healthy",
  "service": "ai-email-generation-adapter",
  "rabbitmq_connected": true,
  "request_queue": "ai.email.generation.requests",
  "response_queue": "ai.email.generation.responses"
}
```

## Integration

### With Other Microservices

Other microservices (campaign-svc, lead-svc, etc.) should:
1. Publish email generation requests to `ai.email.generation.requests` queue
2. Include a unique `request_id` in the message
3. Listen to `ai.email.generation.responses` queue for results
4. Match responses using the `request_id`

### Database Integration

The adapter can query the database (in a separate container) for additional context:
- Company information by `company_id`
- Contact information by `contact_id`
- Campaign requirements by `campaign_id`
- Save generated emails to the `emails` table

The database uses persistent volume storage, so data survives container restarts.

## Docker Deployment

The service is configured in `docker-compose.yml` with:
- RabbitMQ dependency (waits for RabbitMQ to be healthy)
- Database dependency (waits for PostgreSQL to be healthy)
- Environment variables for RabbitMQ and database connection
- Persistent volumes for database storage





