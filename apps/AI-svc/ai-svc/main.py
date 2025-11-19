"""
AI Email Generation Service
Generates personalized cold outreach emails based on user requirements and company/contact data.
"""
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import Optional, List, Dict
import os
from datetime import datetime
import logging

from services.email_generator import EmailGenerator
from services.llm_service import LLMService
from models import CompanyInfo, ContactInfo, EmailRequirements

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="AI Email Generation Service",
    description="Service for generating personalized B2B cold outreach emails",
    version="1.0.0"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # In production, specify exact origins
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize services
llm_service = LLMService()
email_generator = EmailGenerator(llm_service)


# Request/Response Models
class EmailGenerationRequest(BaseModel):
    """Complete request for email generation"""
    company: CompanyInfo
    contact: ContactInfo
    requirements: EmailRequirements
    sequence_step: Optional[int] = Field(1, description="Step number in email sequence (1, 2, 3...)")
    previous_email_context: Optional[str] = Field(None, description="Context from previous email if this is a follow-up")


class EmailGenerationResponse(BaseModel):
    """Response containing generated email"""
    subject: str
    body: str
    personalization_score: Optional[float] = Field(None, description="Score indicating how personalized the email is (0-1)")
    generated_at: str


class BatchEmailGenerationRequest(BaseModel):
    """Request for generating multiple emails"""
    contacts: List[Dict]  # List of {company, contact, requirements}
    requirements: EmailRequirements  # Shared requirements for all emails


class BatchEmailGenerationResponse(BaseModel):
    """Response containing multiple generated emails"""
    emails: List[EmailGenerationResponse]
    total_generated: int


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "ai-email-generation",
        "timestamp": datetime.now().isoformat()
    }


@app.post("/generate-email", response_model=EmailGenerationResponse)
async def generate_email(request: EmailGenerationRequest):
    """
    Generate a single personalized email based on company, contact, and requirements.
    """
    try:
        logger.info(f"Generating email for contact: {request.contact.first_name} {request.contact.last_name} at {request.company.name}")
        
        # Generate email using the email generator service
        email_result = await email_generator.generate_email(
            company=request.company,
            contact=request.contact,
            requirements=request.requirements,
            sequence_step=request.sequence_step,
            previous_context=request.previous_email_context
        )
        
        return EmailGenerationResponse(
            subject=email_result["subject"],
            body=email_result["body"],
            personalization_score=email_result.get("personalization_score"),
            generated_at=datetime.now().isoformat()
        )
    
    except Exception as e:
        logger.error(f"Error generating email: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Failed to generate email: {str(e)}")


@app.post("/generate-batch-emails", response_model=BatchEmailGenerationResponse)
async def generate_batch_emails(request: BatchEmailGenerationRequest):
    """
    Generate multiple personalized emails in batch.
    Useful for campaign initialization.
    """
    try:
        logger.info(f"Generating batch emails for {len(request.contacts)} contacts")
        
        emails = []
        for contact_data in request.contacts:
            try:
                company = CompanyInfo(**contact_data["company"])
                contact = ContactInfo(**contact_data["contact"])
                requirements = request.requirements
                
                email_result = await email_generator.generate_email(
                    company=company,
                    contact=contact,
                    requirements=requirements,
                    sequence_step=1
                )
                
                emails.append(EmailGenerationResponse(
                    subject=email_result["subject"],
                    body=email_result["body"],
                    personalization_score=email_result.get("personalization_score"),
                    generated_at=datetime.now().isoformat()
                ))
            except Exception as e:
                logger.error(f"Error generating email for contact: {str(e)}")
                continue
        
        return BatchEmailGenerationResponse(
            emails=emails,
            total_generated=len(emails)
        )
    
    except Exception as e:
        logger.error(f"Error in batch email generation: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Failed to generate batch emails: {str(e)}")


@app.post("/generate-email-sequence")
async def generate_email_sequence(
    company: CompanyInfo,
    contact: ContactInfo,
    requirements: EmailRequirements,
    num_steps: int = Field(3, ge=1, le=5, description="Number of follow-up emails in sequence")
):
    """
    Generate a complete email sequence (initial email + follow-ups).
    """
    try:
        logger.info(f"Generating email sequence with {num_steps} steps")
        
        sequence = []
        previous_context = None
        
        for step in range(1, num_steps + 1):
            email_result = await email_generator.generate_email(
                company=company,
                contact=contact,
                requirements=requirements,
                sequence_step=step,
                previous_context=previous_context
            )
            
            sequence.append({
                "step": step,
                "subject": email_result["subject"],
                "body": email_result["body"],
                "delay_days": 3 if step == 1 else 5,  # Default delays
                "generated_at": datetime.now().isoformat()
            })
            
            # Use previous email as context for next
            previous_context = f"Previous email subject: {email_result['subject']}\nPrevious email body: {email_result['body']}"
        
        return {
            "sequence": sequence,
            "total_steps": len(sequence),
            "generated_at": datetime.now().isoformat()
        }
    
    except Exception as e:
        logger.error(f"Error generating email sequence: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Failed to generate email sequence: {str(e)}")


@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "service": "AI Email Generation Service",
        "version": "1.0.0",
        "endpoints": {
            "health": "/health",
            "generate_email": "/generate-email",
            "generate_batch_emails": "/generate-batch-emails",
            "generate_email_sequence": "/generate-email-sequence"
        }
    }


if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", "8090"))
    uvicorn.run(app, host="0.0.0.0", port=port)

