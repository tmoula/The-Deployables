"""
AI Email Generation Adapter Service
Acts as an adapter that watches RabbitMQ for email generation requests from microservices
and processes them using AI. This service listens to RabbitMQ queues and matches requests
with microservice requests.
"""
import os
import logging
import signal
import sys
from threading import Thread
from datetime import datetime
from fastapi import FastAPI, HTTPException, Body
from fastapi.middleware.cors import CORSMiddleware
from pydantic import ValidationError
from typing import Optional
import uvicorn
from dotenv import load_dotenv
from pathlib import Path

# Load environment variables from .env file
# Look for .env in the parent directory (AI-svc root)
# Try multiple paths to handle different execution contexts
base_dir = Path(__file__).parent.parent
env_path = base_dir / '.env'
if not env_path.exists():
    # Fallback: try current directory
    env_path = Path('.env')
load_dotenv(dotenv_path=env_path, override=True)

from src.services.rabbitmq_adapter import RabbitMQAdapter
from src.services.email_generator import EmailGenerator
from src.services.ai_adapter import AIAdapter
from src.services.llm_service import LLMService
from src.services.database_service import DatabaseService
from src.services.prospect_service import ProspectService
from src.models import (
    EmailGenerationRequest, 
    EmailGenerationResponse,
    GeneralAIRequest,
    GeneralAIResponse,
    ProspectDiscoveryRequest,
    ProspectDiscoveryResponse,
    PersonalizationHookRequest,
    PersonalizationHookResponse
)

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Initialize RabbitMQ Adapter
adapter = RabbitMQAdapter()

# Initialize services for direct API calls
llm_service = LLMService()
ai_adapter = AIAdapter(llm_service)
email_generator = EmailGenerator(llm_service)
prospect_service = ProspectService(llm_service)
db_service = DatabaseService()

# FastAPI app for health checks and API endpoints
app = FastAPI(
    title="AI Email Generation Service",
    description="REST API and RabbitMQ adapter for AI email generation and general AI tasks",
    version="2.1.0"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Start RabbitMQ consumer on application startup
@app.on_event("startup")
async def startup_event():
    """Start RabbitMQ consumer thread when FastAPI app starts"""
    logger.info("=== AI Service Startup: Initializing services ===")
    logger.info(f"RabbitMQ configuration - Host: {adapter.rabbitmq_host}:{adapter.rabbitmq_port}, Request queue: {adapter.request_queue}, Response queue: {adapter.response_queue}")
    
    consumer_thread = Thread(target=run_rabbitmq_consumer, daemon=True)
    consumer_thread.start()
    logger.info(f"RabbitMQ consumer thread started - Thread ID: {consumer_thread.ident}, Status: {'Alive' if consumer_thread.is_alive() else 'Not alive'}")
    
    # Give the thread a moment to start connecting
    import time
    time.sleep(1)
    thread_status = "Alive" if consumer_thread.is_alive() else "Not alive"
    logger.info(f"AI Service startup complete - RabbitMQ consumer thread status: {thread_status}")

@app.on_event("shutdown")
async def shutdown_event():
    """Cleanup on application shutdown"""
    logger.info("=== AI Service Shutdown: Cleaning up resources ===")
    logger.info("Disconnecting RabbitMQ adapter...")
    adapter.disconnect()
    logger.info("Closing database connections...")
    db_service.close()
    logger.info("AI Service shutdown complete")


@app.get("/health", tags=["Health"])
async def health_check():
    """
    Health check endpoint for service monitoring.
    
    Returns the current health status of the service including:
    - Service status (healthy/disconnected)
    - RabbitMQ connection status
    - Queue information
    
    **Response:**
    - `status`: "healthy" if RabbitMQ is connected, "disconnected" otherwise
    - `service`: Service identifier
    - `rabbitmq_connected`: Boolean indicating RabbitMQ connection status
    - `rabbitmq_consuming`: Boolean indicating if consumer is active
    - `request_queue`: Name of the request queue
    - `response_queue`: Name of the response queue
    """
    # Check if RabbitMQ connection is established
    is_connected = False
    is_consuming = False
    try:
        if adapter.connection is not None:
            is_connected = not adapter.connection.is_closed
        # Also check if consumer is running
        is_consuming = adapter.is_consuming if hasattr(adapter, 'is_consuming') else False
    except Exception as e:
        logger.warning(f"Health check: Error checking RabbitMQ connection - {type(e).__name__}: {str(e)}")
        is_connected = False
        is_consuming = False
    
    status = "healthy" if is_connected else "disconnected"
    logger.debug(f"Health check completed - Status: {status}, RabbitMQ connected: {is_connected}, Consuming: {is_consuming}")
    
    return {
        "status": status,
        "service": "ai-email-generation-service",
        "rabbitmq_connected": is_connected,
        "rabbitmq_consuming": is_consuming,
        "request_queue": adapter.request_queue,
        "response_queue": adapter.response_queue
    }


@app.get("/", tags=["Info"])
async def root():
    """
    Root endpoint with API documentation and service information.
    
    Provides an overview of available endpoints and service configuration.
    Visit `/docs` for interactive API documentation.
    """
    return {
        "service": "AI Email Generation Service",
        "version": "2.1.0",
        "description": "REST API and RabbitMQ adapter for AI email generation and general AI tasks",
        "documentation": {
            "swagger_ui": "/docs",
            "redoc": "/redoc",
            "openapi_spec": "/openapi.json"
        },
        "endpoints": {
            "health": {
                "method": "GET",
                "path": "/health",
                "description": "Service health check and status"
            },
            "generate_email": {
                "method": "POST",
                "path": "/api/v1/emails/generate",
                "description": "Generate personalized email content using AI"
            },
            "general_ai": {
                "method": "POST",
                "path": "/api/v1/ai/process",
                "description": "Process general AI tasks (summarization, analysis, etc.)"
            },
            "prospect_discovery": {
                "method": "POST",
                "path": "/api/v1/prospects/discover",
                "description": "Discover matching company domains based on criteria"
            },
            "personalization_hook": {
                "method": "POST",
                "path": "/api/v1/prospects/personalize",
                "description": "Generate personalized opening hooks for cold emails"
            },
            "test_prompt": {
                "method": "POST",
                "path": "/api/v1/test-prompt",
                "description": "Test prompts interactively with the AI model"
            }
        },
        "queues": {
            "request": adapter.request_queue,
            "response": adapter.response_queue,
            "error": adapter.error_queue
        }
    }


@app.post("/api/v1/emails/generate", response_model=EmailGenerationResponse, tags=["Email Generation"])
async def generate_email(request: EmailGenerationRequest):
    """
    Generate a personalized email using AI.
    
    This endpoint generates personalized cold outreach emails based on company, contact,
    and campaign requirements. The email includes both subject and body with spintax
    support for A/B testing.
    
    **Request Body:**
    - `company`: Company information (name, industry, size, tech stack, etc.)
    - `contact`: Contact information (name, title, email, personalization notes)
    - `requirements`: Email requirements (product description, tone, length, CTA, etc.)
    - `seller`: (Optional) Seller profile information for better personalization
    - `sequence_step`: (Optional) Step number in email sequence (default: 1)
    - `previous_email_context`: (Optional) Context from previous email for follow-ups
    - `campaign_id`: (Optional) Campaign ID for saving to database
    
    **Response:**
    - `success`: Boolean indicating if generation was successful
    - `subject`: Generated email subject line
    - `body`: Generated email body (includes spintax variables)
    - `personalization_score`: Score (0-1) indicating personalization quality
    - `email_id`: Database ID if email was saved (optional)
    - `generated_at`: ISO timestamp of generation
    - `error`: Error message if generation failed
    
    **Example Request:**
    ```json
    {
        "company": {
            "name": "Acme Corp",
            "industry": "Technology",
            "employee_count": 100
        },
        "contact": {
            "first_name": "John",
            "last_name": "Doe",
            "job_title": "CTO"
        },
        "requirements": {
            "product_service_description": "AI-powered analytics platform",
            "tone": "professional",
            "email_length": "medium"
        }
    }
    ```
    """
    try:
        logger.info(f"Email generation request received - Company: {request.company.name}, Contact: {request.contact.first_name or 'N/A'}, Sequence: {request.sequence_step}")
        
        # Generate email using the email generator
        email_result = await email_generator.generate_email(
            company=request.company,
            contact=request.contact,
            requirements=request.requirements,
            seller=request.seller,
            sequence_step=request.sequence_step,
            previous_context=request.previous_email_context
        )
        
        logger.info(f"Email generated successfully - Subject length: {len(email_result.get('subject', ''))}, Body length: {len(email_result.get('body', ''))}, Personalization score: {email_result.get('personalization_score', 0):.2f}")
        
        # Optionally save to database if campaign_id and contact_id are available
        email_id = None
        if request.campaign_id and request.contact.contact_id:
            try:
                email_id = db_service.save_generated_email({
                    "campaign_id": request.campaign_id,
                    "contact_id": request.contact.contact_id,
                    "sequence_step": request.sequence_step,
                    "subject": email_result["subject"],
                    "body": email_result["body"],
                    "status": "scheduled"
                })
                logger.info(f"Email saved to database - Campaign ID: {request.campaign_id}, Email ID: {email_id}")
            except Exception as e:
                logger.warning(f"Failed to save email to database - Campaign ID: {request.campaign_id}, Error: {type(e).__name__}: {str(e)}")
        
        return EmailGenerationResponse(
            success=True,
            subject=email_result["subject"],
            body=email_result["body"],
            personalization_score=email_result.get("personalization_score"),
            email_id=email_id,
            generated_at=datetime.now().isoformat()
        )
        
    except ValidationError as e:
        logger.error(f"Email generation validation error - {type(e).__name__}: {str(e)}")
        raise HTTPException(
            status_code=400,
            detail={
                "error": "Validation failed",
                "message": str(e),
                "field_errors": e.errors() if hasattr(e, 'errors') else None
            }
        )
    except Exception as e:
        logger.error(f"Email generation failed - {type(e).__name__}: {str(e)}", exc_info=True)
        return EmailGenerationResponse(
            success=False,
            error=f"{type(e).__name__}: {str(e)}",
            generated_at=datetime.now().isoformat()
        )


@app.post("/api/v1/ai/process", response_model=GeneralAIResponse, tags=["AI Processing"])
async def process_ai_task(request: GeneralAIRequest):
    """
    Process a general AI task.
    
    This is a flexible endpoint that can handle any AI task, not just email generation.
    Useful for various AI-powered features like summarization, analysis, content generation, etc.
    
    **Request Body:**
    - `prompt`: (Required) Input prompt/question for the AI
    - `task`: (Optional) Task description or instruction
    - `system_prompt`: (Optional) System-level instructions for the AI
    - `temperature`: (Optional) Temperature setting 0-1 (default: 0.7). Higher = more creative
    - `max_tokens`: (Optional) Maximum tokens to generate (default: 1000)
    - `context`: (Optional) Additional context data as key-value pairs
    
    **Response:**
    - `success`: Boolean indicating if processing was successful
    - `output`: AI-generated response text
    - `error`: Error message if processing failed
    
    **Example Request:**
    ```json
    {
        "prompt": "Summarize the key benefits of cloud computing",
        "task": "Create a brief summary",
        "temperature": 0.7,
        "max_tokens": 500
    }
    ```
    """
    try:
        task_desc = request.task or "General AI task"
        prompt_preview = request.prompt[:50] + "..." if len(request.prompt) > 50 else request.prompt
        logger.info(f"General AI task request received - Task: {task_desc}, Prompt preview: {prompt_preview}, Temperature: {request.temperature}, Max tokens: {request.max_tokens}")
        
        # Prepare input data for AI adapter
        input_data = {
            "task": request.task,
            "prompt": request.prompt,
            "system_prompt": request.system_prompt,
            "temperature": request.temperature,
            "max_tokens": request.max_tokens,
            "context": request.context
        }
        
        # Process through AI adapter
        result = await ai_adapter.process_input(input_data)
        
        if result["success"]:
            output_length = len(result.get("output", ""))
            logger.info(f"General AI task completed successfully - Output length: {output_length} characters")
            return GeneralAIResponse(
                success=True,
                output=result["output"]
            )
        else:
            error_msg = result.get("error", "Unknown error occurred")
            logger.error(f"General AI task failed - Error: {error_msg}")
            return GeneralAIResponse(
                success=False,
                error=error_msg
            )
            
    except ValidationError as e:
        logger.error(f"General AI task validation error - {type(e).__name__}: {str(e)}")
        raise HTTPException(
            status_code=400,
            detail={
                "error": "Validation failed",
                "message": str(e),
                "field_errors": e.errors() if hasattr(e, 'errors') else None
            }
        )
    except Exception as e:
        logger.error(f"General AI task processing failed - {type(e).__name__}: {str(e)}", exc_info=True)
        return GeneralAIResponse(
            success=False,
            error=f"{type(e).__name__}: {str(e)}"
        )


@app.post("/api/v1/prospects/discover", response_model=ProspectDiscoveryResponse, tags=["Prospect Discovery"])
async def discover_prospects(request: ProspectDiscoveryRequest):
    """
    Discover matching companies/prospects based on criteria using AI.
    
    This endpoint uses AI to generate a list of company domains that match
    the provided search criteria. Useful for finding potential prospects
    based on industry, size, location, tech stack, and other attributes.
    
    **Request Body:**
    - `criteria`: Search criteria object with fields like:
        - `industry`: Industry sector
        - `min_size` / `max_size`: Company size range (employee count)
        - `domain`: Specific domain to search
        - `tech_used`: List of technologies used
        - `regions`: Geographic regions
        - And many more optional fields
    - `max_companies`: Maximum number of companies to return (max: 5)
    
    **Response:**
    - `success`: Boolean indicating if discovery was successful
    - `company_domains`: List of company domain names (e.g., ["example.com", "company.com"])
    - `error`: Error message if discovery failed
    
    **Example Request:**
    ```json
    {
        "criteria": {
            "industry": "Technology",
            "min_size": 50,
            "max_size": 500,
            "tech_used": ["Python", "AWS"]
        },
        "max_companies": 5
    }
    ```
    
    **Note:** The maximum number of companies returned is limited to 5 for performance reasons.
    """
    try:
        logger.info(f"Prospect discovery request received - Industry: {request.criteria.industry or 'Any'}, Size range: {request.criteria.min_size or 'N/A'}-{request.criteria.max_size or 'N/A'}, Requested companies: {request.max_companies}")
        
        # Enforce maximum of 5 companies
        max_companies = min(request.max_companies, 5)
        if request.max_companies > 5:
            logger.warning(f"Prospect discovery: Requested {request.max_companies} companies exceeds limit, capping at 5")
        
        # Generate matching companies using prospect service
        logger.info(f"Prospect discovery: Generating {max_companies} matching company domains")
        company_domains = await prospect_service.generate_matching_companies(
            criteria=request.criteria,
            max_companies=max_companies
        )
        
        logger.info(f"Prospect discovery completed successfully - Found {len(company_domains)} company domains: {', '.join(company_domains) if company_domains else 'None'}")
        
        return ProspectDiscoveryResponse(
            success=True,
            company_domains=company_domains
        )
        
    except ValidationError as e:
        logger.error(f"Prospect discovery validation error - {type(e).__name__}: {str(e)}")
        raise HTTPException(
            status_code=400,
            detail={
                "error": "Validation failed",
                "message": str(e),
                "field_errors": e.errors() if hasattr(e, 'errors') else None
            }
        )
    except Exception as e:
        logger.error(f"Prospect discovery failed - {type(e).__name__}: {str(e)}", exc_info=True)
        return ProspectDiscoveryResponse(
            success=False,
            error=f"{type(e).__name__}: {str(e)}"
        )


@app.post("/api/v1/test-prompt", tags=["Testing"])
async def test_prompt(
    prompt: str = Body(..., description="The prompt to test"),
    system_prompt: str = Body(None, description="System-level instructions for the AI"),
    temperature: float = Body(0.7, ge=0.0, le=2.0, description="Temperature setting (0-2)"),
    max_tokens: int = Body(1000, ge=1, le=4000, description="Maximum tokens to generate (1-4000)")
):
    """
    Test prompts interactively with the AI model.
    
    This endpoint is useful for testing and debugging prompts before using them
    in production. It provides a simple interface to test how the AI responds
    to different prompts and configurations.
    
    **Request Body (JSON):**
    - `prompt`: (Required) The prompt to test
    - `system_prompt`: (Optional) System-level instructions for the AI
    - `temperature`: (Optional) Temperature setting 0-2 (default: 0.7)
    - `max_tokens`: (Optional) Maximum tokens to generate 1-4000 (default: 1000)
    
    **Response:**
    - `success`: Boolean indicating if processing was successful
    - `output`: AI-generated response text
    - `input`: Echo of the input parameters
    - `error`: Error message if processing failed
    
    **Example Request:**
    ```json
    {
        "prompt": "Explain machine learning in simple terms",
        "system_prompt": "You are a helpful teacher",
        "temperature": 0.8,
        "max_tokens": 500
    }
    ```
    """
    try:
        logger.info(f"Test prompt request received - Prompt length: {len(prompt)}, Temperature: {temperature}, Max tokens: {max_tokens}")
        
        # Prepare input data for AI adapter
        input_data = {
            "prompt": prompt,
            "system_prompt": system_prompt,
            "temperature": temperature,
            "max_tokens": max_tokens
        }
        
        # Process through AI adapter
        result = await ai_adapter.process_input(input_data)
        
        if result["success"]:
            output_length = len(result.get("output", ""))
            logger.info(f"Test prompt completed successfully - Output length: {output_length} characters")
            return {
                "success": True,
                "output": result["output"],
                "input": {
                    "prompt": prompt[:100] + "..." if len(prompt) > 100 else prompt,
                    "system_prompt": system_prompt,
                    "temperature": temperature,
                    "max_tokens": max_tokens
                }
            }
        else:
            error_msg = result.get("error", "Unknown error occurred")
            logger.error(f"Test prompt failed - Error: {error_msg}")
            return {
                "success": False,
                "error": error_msg,
                "input": {
                    "prompt": prompt[:100] + "..." if len(prompt) > 100 else prompt,
                    "system_prompt": system_prompt,
                    "temperature": temperature,
                    "max_tokens": max_tokens
                }
            }
            
    except Exception as e:
        logger.error(f"Test prompt processing failed - {type(e).__name__}: {str(e)}", exc_info=True)
        return {
            "success": False,
            "error": f"{type(e).__name__}: {str(e)}",
            "input": {
                "prompt": prompt[:100] + "..." if len(prompt) > 100 else prompt,
                "system_prompt": system_prompt,
                "temperature": temperature,
                "max_tokens": max_tokens
            }
        }


@app.post("/api/v1/test-prompt-json", tags=["Testing"])
async def test_prompt_json(
    prompt: str = Body(..., description="The prompt to test"),
    system_prompt: Optional[str] = Body(None, description="System-level instructions"),
    temperature: float = Body(0.7, ge=0.0, le=2.0, description="Temperature setting (0-2)"),
    max_tokens: int = Body(1000, ge=1, le=4000, description="Maximum tokens to generate (1-4000)")
):
    """
    Test prompts interactively with the AI model (JSON body version).
    
    This is an alternative version of the test-prompt endpoint that accepts
    JSON in the request body instead of query parameters. Use this for
    more complex prompts or when you prefer JSON over query parameters.
    
    **Request Body (JSON):**
    - `prompt`: (Required) The prompt to test
    - `system_prompt`: (Optional) System-level instructions for the AI
    - `temperature`: (Optional) Temperature setting 0-2 (default: 0.7)
    - `max_tokens`: (Optional) Maximum tokens to generate 1-4000 (default: 1000)
    
    **Response:**
    - `success`: Boolean indicating if processing was successful
    - `output`: AI-generated response text
    - `input`: Echo of the input parameters
    - `error`: Error message if processing failed
    
    **Example Request:**
    ```json
    {
        "prompt": "Explain machine learning in simple terms",
        "system_prompt": "You are a helpful teacher",
        "temperature": 0.8,
        "max_tokens": 500
    }
    ```
    """
    try:
        logger.info(f"Test prompt (JSON) request received - Prompt length: {len(prompt)}, Temperature: {temperature}, Max tokens: {max_tokens}")
        
        # Prepare input data for AI adapter
        input_data = {
            "prompt": prompt,
            "system_prompt": system_prompt,
            "temperature": temperature,
            "max_tokens": max_tokens
        }
        
        # Process through AI adapter
        result = await ai_adapter.process_input(input_data)
        
        if result["success"]:
            output_length = len(result.get("output", ""))
            logger.info(f"Test prompt (JSON) completed successfully - Output length: {output_length} characters")
            return {
                "success": True,
                "output": result["output"],
                "input": {
                    "prompt": prompt[:100] + "..." if len(prompt) > 100 else prompt,
                    "system_prompt": system_prompt,
                    "temperature": temperature,
                    "max_tokens": max_tokens
                }
            }
        else:
            error_msg = result.get("error", "Unknown error occurred")
            logger.error(f"Test prompt (JSON) failed - Error: {error_msg}")
            return {
                "success": False,
                "error": error_msg,
                "input": {
                    "prompt": prompt[:100] + "..." if len(prompt) > 100 else prompt,
                    "system_prompt": system_prompt,
                    "temperature": temperature,
                    "max_tokens": max_tokens
                }
            }
            
    except Exception as e:
        logger.error(f"Test prompt (JSON) processing failed - {type(e).__name__}: {str(e)}", exc_info=True)
        return {
            "success": False,
            "error": f"{type(e).__name__}: {str(e)}",
            "input": {
                "prompt": prompt[:100] + "..." if len(prompt) > 100 else prompt,
                "system_prompt": system_prompt,
                "temperature": temperature,
                "max_tokens": max_tokens
            }
        }


@app.post("/api/v1/prospects/personalize", response_model=PersonalizationHookResponse, tags=["Prospect Personalization"])
async def generate_personalization_hook(request: PersonalizationHookRequest):
    """
    Generate a personalization hook for a prospect based on company and seller information.
    
    This endpoint creates a highly personalized opening hook (1-2 sentences) for cold outreach
    emails that shows deep knowledge of the prospect's company. The hook is designed to
    be inserted at the beginning of cold emails to increase engagement.
    
    **Request Body:**
    - `prospect`: Prospect information including:
        - `company`: Company name (required)
        - `domain`: Company domain (required)
        - `first_name`, `last_name`: Contact name
        - `position`: Job title
        - `industry`, `size`: Company details
        - `stack`, `keywords`: Additional context
    - `seller`: Seller profile information including:
        - `company_name`: Seller's company name (required)
        - `industry`: Seller's industry (required)
        - `value_proposition_keywords`: Key value propositions
        - `tech_stack`: Technologies used
        - And other optional fields
    
    **Response:**
    - `success`: Boolean indicating if generation was successful
    - `hook`: Personalized hook text (1-2 sentences, max 120 characters)
    - `error`: Error message if generation failed
    
    **Example Request:**
    ```json
    {
        "prospect": {
            "company": "Acme Corp",
            "domain": "acme.com",
            "industry": "Technology",
            "size": 200
        },
        "seller": {
            "company_name": "TechSolutions Inc",
            "industry": "SaaS",
            "value_proposition_keywords": ["automation", "efficiency"]
        }
    }
    ```
    
    **Example Response:**
    ```json
    {
        "success": true,
        "hook": "Acme Corp's recent expansion into cloud infrastructure aligns perfectly with our automation solutions."
    }
    ```
    """
    try:
        logger.info(f"Personalization hook request received - Prospect company: {request.prospect.company}, Seller: {request.seller.company_name}")
        
        # Generate personalization hook using prospect service
        hook = await prospect_service.generate_personalization_hook(
            prospect=request.prospect,
            seller=request.seller
        )
        
        hook_length = len(hook) if hook else 0
        logger.info(f"Personalization hook generated successfully - Hook length: {hook_length} characters, Preview: {hook[:50] if hook else 'N/A'}...")
        
        return PersonalizationHookResponse(
            success=True,
            hook=hook
        )
        
    except ValidationError as e:
        logger.error(f"Personalization hook validation error - {type(e).__name__}: {str(e)}")
        raise HTTPException(
            status_code=400,
            detail={
                "error": "Validation failed",
                "message": str(e),
                "field_errors": e.errors() if hasattr(e, 'errors') else None
            }
        )
    except Exception as e:
        logger.error(f"Personalization hook generation failed - {type(e).__name__}: {str(e)}", exc_info=True)
        return PersonalizationHookResponse(
            success=False,
            error=f"{type(e).__name__}: {str(e)}"
        )


def signal_handler(sig, frame):
    """Handle shutdown signals gracefully"""
    logger.info(f"=== Shutdown signal received ({sig}): Initiating graceful shutdown ===")
    logger.info("Disconnecting RabbitMQ adapter...")
    adapter.disconnect()
    logger.info("Closing database connections...")
    db_service.close()
    logger.info("Shutdown complete - Exiting")
    sys.exit(0)


def run_rabbitmq_consumer():
    """Run RabbitMQ consumer in a separate thread"""
    try:
        logger.info("=== RabbitMQ Consumer Thread: Starting ===")
        logger.info(f"Connecting to RabbitMQ - Host: {adapter.rabbitmq_host}:{adapter.rabbitmq_port}, Queue: {adapter.request_queue}")
        adapter.start_consuming()
    except Exception as e:
        logger.error(f"RabbitMQ consumer thread error - {type(e).__name__}: {str(e)}", exc_info=True)
        sys.exit(1)


if __name__ == "__main__":
    # Register signal handlers for graceful shutdown
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
    
    # Start FastAPI server for API endpoints and health checks
    # Note: RabbitMQ consumer is started via @app.on_event("startup")
    port = int(os.getenv("PORT", "8090"))
    logger.info(f"=== Starting AI Service API Server ===")
    logger.info(f"Server configuration - Host: 0.0.0.0, Port: {port}")
    logger.info("Service ready - Listening for RabbitMQ messages and API requests")
    logger.info(f"API documentation available at: http://0.0.0.0:{port}/docs")
    
    uvicorn.run(app, host="0.0.0.0", port=port, log_level="info")

