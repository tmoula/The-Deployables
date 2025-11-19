# AI Email Generation Service

This service generates personalized B2B cold outreach emails using AI (OpenAI or Ollama).

## Features

- **Personalized Email Generation**: Creates customized emails based on company and contact information
- **Email Sequences**: Generates complete follow-up sequences
- **Batch Processing**: Generate multiple emails at once
- **Flexible LLM Support**: Works with OpenAI API or local Ollama instances
- **Configurable**: Customize tone, length, and requirements

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

- `OPENAI_API_KEY`: Your OpenAI API key (required if not using Ollama)
- `OPENAI_MODEL`: Model to use (default: gpt-4o-mini)
- `USE_OLLAMA`: Set to "true" to use Ollama instead of OpenAI
- `OLLAMA_URL`: Ollama server URL (default: http://localhost:11434)
- `OLLAMA_MODEL`: Ollama model name (default: llama3.2)
- `PORT`: Service port (default: 8090)

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

## API Endpoints

### Health Check
```
GET /health
```

### Generate Single Email
```
POST /generate-email
```

Request body:
```json
{
  "company": {
    "name": "Acme Corp",
    "industry": "Technology",
    "employee_count": 150,
    "tech_stack": ["Python", "AWS"]
  },
  "contact": {
    "first_name": "John",
    "last_name": "Doe",
    "job_title": "CTO"
  },
  "requirements": {
    "product_service_description": "AI-powered analytics platform",
    "value_proposition": "Reduce operational costs by 30%",
    "target_pain_points": ["high costs", "manual processes"],
    "tone": "professional",
    "email_length": "medium"
  },
  "sequence_step": 1
}
```

### Generate Batch Emails
```
POST /generate-batch-emails
```

### Generate Email Sequence
```
POST /generate-email-sequence
```

## Integration

The service is designed to integrate with the campaign service and database. Generated emails can be stored in the `emails` table with the following structure:

- `final_subject`: Generated subject line
- `final_body`: Generated email body
- `campaign_id`: Associated campaign
- `contact_id`: Target contact
- `sequence_step`: Step in email sequence




