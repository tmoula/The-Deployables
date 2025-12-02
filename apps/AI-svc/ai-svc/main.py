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

from services.rabbitmq_adapter import RabbitMQAdapter
from services.email_generator import EmailGenerator
from services.ai_adapter import AIAdapter
from services.llm_service import LLMService
from services.database_service import DatabaseService
from services.prospect_service import ProspectService
from models import (
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


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    is_connected = adapter.connection is not None and not adapter.connection.is_closed if adapter.connection else False
    return {
        "status": "healthy" if is_connected else "disconnected",
        "service": "ai-email-generation-service",
        "rabbitmq_connected": is_connected,
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
        logger.info(f"Received prospect discovery request via API")
        
        # Generate matching companies using prospect service
        company_domains = await prospect_service.generate_matching_companies(
            criteria=request.criteria,
            max_companies=request.max_companies
        )
        
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
        adapter.start_consuming()
    except Exception as e:
        logger.error(f"Error in RabbitMQ consumer: {str(e)}", exc_info=True)
        sys.exit(1)


if __name__ == "__main__":
    # Register signal handlers for graceful shutdown
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
    
    # Start RabbitMQ consumer in a separate thread
    consumer_thread = Thread(target=run_rabbitmq_consumer, daemon=True)
    consumer_thread.start()
    
    # Start FastAPI server for API endpoints and health checks
    port = int(os.getenv("PORT", "8090"))
    logger.info(f"Starting AI Service API server on port {port}")
    logger.info("AI Service is running. Listening for RabbitMQ messages and API requests...")
    
    uvicorn.run(app, host="0.0.0.0", port=port, log_level="info")

