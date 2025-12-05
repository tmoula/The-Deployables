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
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import ValidationError
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
    logger.info("=== FastAPI Startup Event: Initializing RabbitMQ Adapter ===")
    logger.info(f"RabbitMQ Host: {adapter.rabbitmq_host}:{adapter.rabbitmq_port}")
    logger.info(f"Request Queue: {adapter.request_queue}")
    
    consumer_thread = Thread(target=run_rabbitmq_consumer, daemon=True)
    consumer_thread.start()
    logger.info(f"RabbitMQ consumer thread started. Thread ID: {consumer_thread.ident}, Alive: {consumer_thread.is_alive()}")
    
    # Give the thread a moment to start connecting
    import time
    time.sleep(1)
    logger.info(f"After 1 second - Thread alive: {consumer_thread.is_alive()}")

@app.on_event("shutdown")
async def shutdown_event():
    """Cleanup on application shutdown"""
    logger.info("FastAPI shutdown event: Disconnecting RabbitMQ adapter...")
    adapter.disconnect()
    db_service.close()


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    # Check if RabbitMQ connection is established
    is_connected = False
    try:
        if adapter.connection is not None:
            is_connected = not adapter.connection.is_closed
        # Also check if consumer is running
        is_consuming = adapter.is_consuming if hasattr(adapter, 'is_consuming') else False
    except Exception as e:
        logger.warning(f"Error checking RabbitMQ connection: {e}")
        is_connected = False
        is_consuming = False
    
    return {
        "status": "healthy" if is_connected else "disconnected",
        "service": "ai-email-generation-service",
        "rabbitmq_connected": is_connected,
        "rabbitmq_consuming": is_consuming,
        "request_queue": adapter.request_queue,
        "response_queue": adapter.response_queue
    }


@app.get("/")
async def root():
    """Root endpoint with API documentation"""
    return {
        "service": "AI Email Generation Service",
        "version": "2.1.0",
        "description": "REST API and RabbitMQ adapter for AI email generation",
        "endpoints": {
            "health": "/health",
            "generate_email": "POST /api/v1/emails/generate",
            "general_ai": "POST /api/v1/ai/process",
            "prospect_discovery": "POST /api/v1/prospects/discover",
            "personalization_hook": "POST /api/v1/prospects/personalize"
        },
        "queues": {
            "request": adapter.request_queue,
            "response": adapter.response_queue,
            "error": adapter.error_queue
        }
    }


@app.post("/api/v1/emails/generate", response_model=EmailGenerationResponse)
async def generate_email(request: EmailGenerationRequest):
    """
    Generate a personalized email using AI.
    
    This endpoint directly processes email generation requests via REST API,
    in addition to the RabbitMQ queue processing.
    """
    try:
        logger.info(f"Received email generation request via API")
        
        # Generate email using the email generator
        email_result = await email_generator.generate_email(
            company=request.company,
            contact=request.contact,
            requirements=request.requirements,
            seller=request.seller,
            sequence_step=request.sequence_step,
            previous_context=request.previous_email_context
        )
        
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
                logger.debug(f"Saved generated email to database: email_id={email_id}")
            except Exception as e:
                logger.warning(f"Failed to save email to database: {str(e)}")
        
        return EmailGenerationResponse(
            success=True,
            subject=email_result["subject"],
            body=email_result["body"],
            personalization_score=email_result.get("personalization_score"),
            email_id=email_id,
            generated_at=datetime.now().isoformat()
        )
        
    except ValidationError as e:
        logger.error(f"Validation error: {str(e)}")
        raise HTTPException(status_code=400, detail=f"Invalid request: {str(e)}")
    except Exception as e:
        logger.error(f"Error generating email: {str(e)}", exc_info=True)
        return EmailGenerationResponse(
            success=False,
            error=str(e),
            generated_at=datetime.now().isoformat()
        )


@app.post("/api/v1/ai/process", response_model=GeneralAIResponse)
async def process_ai_task(request: GeneralAIRequest):
    """
    Process a general AI task.
    
    This is a flexible endpoint that can handle any AI task, not just email generation.
    Useful for various AI-powered features like summarization, analysis, etc.
    """
    try:
        logger.info(f"Received general AI task request via API")
        
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
            return GeneralAIResponse(
                success=True,
                output=result["output"]
            )
        else:
            return GeneralAIResponse(
                success=False,
                error=result.get("error", "Unknown error occurred")
            )
            
    except ValidationError as e:
        logger.error(f"Validation error: {str(e)}")
        raise HTTPException(status_code=400, detail=f"Invalid request: {str(e)}")
    except Exception as e:
        logger.error(f"Error processing AI task: {str(e)}", exc_info=True)
        return GeneralAIResponse(
            success=False,
            error=str(e)
        )


@app.post("/api/v1/prospects/discover", response_model=ProspectDiscoveryResponse)
async def discover_prospects(request: ProspectDiscoveryRequest):
    """
    Discover matching companies/prospects based on criteria using AI.
    
    This endpoint uses AI to generate a list of company domains that match
    the provided search criteria.
    """
    try:
        logger.info(f"=== PROSPECT DISCOVERY REQUEST ===")
        logger.info(f"Criteria: industry={request.criteria.industry}, min_size={request.criteria.min_size}, max_size={request.criteria.max_size}")
        logger.info(f"Requested max_companies: {request.max_companies}")
        
        # Enforce maximum of 5 companies
        max_companies = min(request.max_companies, 5)
        if request.max_companies > 5:
            logger.warning(f"Requested {request.max_companies} companies, limiting to 5")
        
        # Generate matching companies using prospect service
        logger.info(f"Calling prospect_service.generate_matching_companies with max_companies={max_companies}")
        company_domains = await prospect_service.generate_matching_companies(
            criteria=request.criteria,
            max_companies=max_companies
        )
        
        logger.info(f"=== PROSPECT DISCOVERY RESULT ===")
        logger.info(f"Generated {len(company_domains)} company domains: {company_domains}")
        
        return ProspectDiscoveryResponse(
            success=True,
            company_domains=company_domains
        )
        
    except ValidationError as e:
        logger.error(f"Validation error: {str(e)}")
        raise HTTPException(status_code=400, detail=f"Invalid request: {str(e)}")
    except Exception as e:
        logger.error(f"Error discovering prospects: {str(e)}", exc_info=True)
        return ProspectDiscoveryResponse(
            success=False,
            error=str(e)
        )


@app.post("/api/v1/prospects/personalize", response_model=PersonalizationHookResponse)
async def generate_personalization_hook(request: PersonalizationHookRequest):
    """
    Generate a personalization hook for a prospect based on company and seller information.
    
    This endpoint creates a highly personalized opening hook for cold outreach emails
    that shows deep knowledge of the prospect's company.
    """
    try:
        logger.info(f"Received personalization hook request via API for {request.prospect.company}")
        
        # Generate personalization hook using prospect service
        hook = await prospect_service.generate_personalization_hook(
            prospect=request.prospect,
            seller=request.seller
        )
        
        return PersonalizationHookResponse(
            success=True,
            hook=hook
        )
        
    except ValidationError as e:
        logger.error(f"Validation error: {str(e)}")
        raise HTTPException(status_code=400, detail=f"Invalid request: {str(e)}")
    except Exception as e:
        logger.error(f"Error generating personalization hook: {str(e)}", exc_info=True)
        return PersonalizationHookResponse(
            success=False,
            error=str(e)
        )


def signal_handler(sig, frame):
    """Handle shutdown signals gracefully"""
    logger.info("Shutdown signal received. Stopping adapter...")
    adapter.disconnect()
    db_service.close()
    sys.exit(0)


def run_rabbitmq_consumer():
    """Run RabbitMQ consumer in a separate thread"""
    try:
        logger.info("=== Starting RabbitMQ consumer thread ===")
        logger.info(f"Connecting to RabbitMQ at {adapter.rabbitmq_host}:{adapter.rabbitmq_port}")
        logger.info(f"Will listen on queue: {adapter.request_queue}")
        adapter.start_consuming()
    except Exception as e:
        logger.error(f"Error in RabbitMQ consumer: {str(e)}", exc_info=True)
        sys.exit(1)


if __name__ == "__main__":
    # Register signal handlers for graceful shutdown
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
    
    # Start FastAPI server for API endpoints and health checks
    # Note: RabbitMQ consumer is started via @app.on_event("startup")
    port = int(os.getenv("PORT", "8090"))
    logger.info(f"Starting AI Service API server on port {port}")
    logger.info("AI Service is running. Listening for RabbitMQ messages and API requests...")
    
    uvicorn.run(app, host="0.0.0.0", port=port, log_level="info")

