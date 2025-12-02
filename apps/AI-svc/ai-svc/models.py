"""
Pydantic models for request/response validation
"""
from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any


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


class SellerInfo(BaseModel):
    """Seller profile information"""
    company_name: Optional[str] = None
    industry: Optional[str] = None
    company_size: Optional[int] = None
    value_proposition_keywords: Optional[List[str]] = None
    target_customer_segment: Optional[str] = None
    tech_stack: Optional[List[str]] = None
    sales_model: Optional[str] = None
    target_regions: Optional[List[str]] = None

class EmailGenerationRequest(BaseModel):
    """Request model for email generation API"""
    company: CompanyInfo
    contact: ContactInfo
    requirements: EmailRequirements
    seller: Optional[SellerInfo] = Field(None, description="Seller profile information")
    sequence_step: Optional[int] = Field(1, description="Step number in email sequence")
    previous_email_context: Optional[str] = Field(None, description="Context from previous email")
    campaign_id: Optional[int] = Field(None, description="Campaign ID for saving email")


class EmailGenerationResponse(BaseModel):
    """Response model for email generation API"""
    success: bool
    subject: Optional[str] = None
    body: Optional[str] = None
    personalization_score: Optional[float] = None
    email_id: Optional[int] = None
    error: Optional[str] = None
    generated_at: Optional[str] = None


class GeneralAIRequest(BaseModel):
    """Request model for general AI tasks"""
    task: Optional[str] = Field(None, description="Task description or instruction")
    prompt: str = Field(..., description="Input prompt/question for the AI")
    system_prompt: Optional[str] = Field(None, description="System-level instructions")
    temperature: Optional[float] = Field(0.7, description="Temperature setting (0-1)")
    max_tokens: Optional[int] = Field(1000, description="Maximum tokens to generate")
    context: Optional[Dict[str, Any]] = Field(None, description="Additional context data")


class GeneralAIResponse(BaseModel):
    """Response model for general AI tasks"""
    success: bool
    output: Optional[str] = None
    error: Optional[str] = None


class ProspectCriteria(BaseModel):
    """Criteria for prospect discovery"""
    company_name: Optional[str] = None
    domain: Optional[str] = None
    industry: Optional[str] = None
    min_size: Optional[int] = None
    max_size: Optional[int] = None
    min_founded_year: Optional[int] = None
    max_founded_year: Optional[int] = None
    funding_stage: Optional[str] = None
    annual_revenue_range: Optional[str] = None
    growth_rate: Optional[str] = None
    tech_used: Optional[List[str]] = None
    tech_category: Optional[str] = None
    target_roles: Optional[List[str]] = None
    seniority_level: Optional[str] = None
    headquarters_region: Optional[str] = None
    hq_country: Optional[str] = None
    remote_friendly: Optional[bool] = None
    regions: Optional[List[str]] = None
    hiring_trends: Optional[str] = None
    recent_tech_adoption: Optional[str] = None
    keyword_mentions: Optional[List[str]] = None


class ProspectDiscoveryRequest(BaseModel):
    """Request model for prospect discovery"""
    criteria: ProspectCriteria
    max_companies: int = Field(5, description="Maximum number of companies to return")


class ProspectDiscoveryResponse(BaseModel):
    """Response model for prospect discovery"""
    success: bool
    company_domains: Optional[List[str]] = None
    error: Optional[str] = None


class SellerProfile(BaseModel):
    """Seller profile information"""
    company_name: str
    industry: str
    company_size: Optional[int] = None
    founded_year: Optional[int] = None
    headquarters_region: Optional[str] = None
    value_proposition_keywords: Optional[List[str]] = None
    target_customer_segment: Optional[str] = None
    price_tier: Optional[str] = None
    tech_stack: Optional[List[str]] = None
    sales_model: Optional[str] = None
    target_regions: Optional[List[str]] = None


class ProspectInfo(BaseModel):
    """Prospect information for personalization"""
    id: Optional[str] = None
    company: str
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    position: Optional[str] = None
    email: Optional[str] = None
    domain: str
    industry: Optional[str] = None
    size: Optional[int] = None
    regions: Optional[List[str]] = None
    stack: Optional[List[str]] = None
    keywords: Optional[List[str]] = None


class PersonalizationHookRequest(BaseModel):
    """Request model for personalization hook generation"""
    prospect: ProspectInfo
    seller: SellerProfile


class PersonalizationHookResponse(BaseModel):
    """Response model for personalization hook generation"""
    success: bool
    hook: Optional[str] = None
    error: Optional[str] = None




