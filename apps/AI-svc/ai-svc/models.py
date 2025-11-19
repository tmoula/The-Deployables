"""
Pydantic models for request/response validation
"""
from pydantic import BaseModel, Field
from typing import Optional, List


class CompanyInfo(BaseModel):
    """Company information for personalization"""
    company_id: Optional[int] = None
    name: str
    website: Optional[str] = None
    industry: Optional[str] = None
    employee_count: Optional[int] = None
    location: Optional[str] = None
    tech_stack: Optional[List[str]] = None
    enrichment_notes: Optional[str] = None


class ContactInfo(BaseModel):
    """Contact information for personalization"""
    contact_id: Optional[int] = None
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    job_title: Optional[str] = None
    email: Optional[str] = None
    personalization_notes: Optional[str] = None


class EmailRequirements(BaseModel):
    """User requirements for email generation"""
    campaign_name: Optional[str] = None
    product_service_description: str = Field(..., description="Description of the product/service being offered")
    value_proposition: Optional[str] = Field(None, description="Main value proposition")
    target_pain_points: Optional[List[str]] = Field(None, description="Pain points to address")
    tone: Optional[str] = Field("professional", description="Email tone: professional, casual, friendly, formal")
    email_length: Optional[str] = Field("medium", description="Email length: short, medium, long")
    include_call_to_action: Optional[bool] = Field(True, description="Include CTA in email")
    custom_instructions: Optional[str] = Field(None, description="Additional custom instructions")




