"""
Pydantic models for request/response validation
"""
from pydantic import BaseModel, Field, field_validator, EmailStr
from typing import Optional, List, Dict, Any
import re


class CompanyInfo(BaseModel):
    """Company information for personalization"""
    company_id: Optional[int] = Field(None, ge=1, description="Company ID must be positive")
    name: str = Field(..., min_length=1, max_length=200, description="Company name (1-200 characters)")
    website: Optional[str] = Field(None, description="Company website URL")
    industry: Optional[str] = Field(None, max_length=100, description="Industry sector (max 100 characters)")
    employee_count: Optional[int] = Field(None, ge=1, description="Employee count must be positive")
    location: Optional[str] = Field(None, max_length=200, description="Company location (max 200 characters)")
    tech_stack: Optional[List[str]] = Field(None, max_length=20, description="Technology stack (max 20 items)")
    enrichment_notes: Optional[str] = Field(None, max_length=2000, description="Enrichment notes (max 2000 characters)")
    
    @field_validator('website')
    @classmethod
    def validate_website(cls, v):
        """Validate website URL format if provided"""
        if v is not None and v.strip():
            # Basic URL validation
            url_pattern = r'^https?://[^\s/$.?#].[^\s]*$'
            if not re.match(url_pattern, v):
                # Try adding http:// if no protocol
                if not v.startswith(('http://', 'https://')):
                    v = 'https://' + v
        return v


class ContactInfo(BaseModel):
    """Contact information for personalization"""
    contact_id: Optional[int] = Field(None, ge=1, description="Contact ID must be positive")
    first_name: Optional[str] = Field(None, max_length=100, description="First name (max 100 characters)")
    last_name: Optional[str] = Field(None, max_length=100, description="Last name (max 100 characters)")
    job_title: Optional[str] = Field(None, max_length=200, description="Job title (max 200 characters)")
    email: Optional[str] = Field(None, description="Contact email address")
    personalization_notes: Optional[str] = Field(None, max_length=1000, description="Personalization notes (max 1000 characters)")
    
    @field_validator('email')
    @classmethod
    def validate_email(cls, v):
        """Validate email format if provided"""
        if v is not None and v.strip():
            # Basic email validation pattern
            email_pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
            if not re.match(email_pattern, v):
                raise ValueError('Invalid email format')
        return v


class EmailRequirements(BaseModel):
    """User requirements for email generation"""
    campaign_name: Optional[str] = Field(None, max_length=200, description="Campaign name (max 200 characters)")
    product_service_description: str = Field(..., min_length=10, max_length=2000, description="Description of the product/service being offered (10-2000 characters)")
    value_proposition: Optional[str] = Field(None, max_length=500, description="Main value proposition (max 500 characters)")
    target_pain_points: Optional[List[str]] = Field(None, max_length=10, description="Pain points to address (max 10 items)")
    tone: Optional[str] = Field("professional", description="Email tone: professional, casual, friendly, formal")
    email_length: Optional[str] = Field("medium", description="Email length: short, medium, long")
    include_call_to_action: Optional[bool] = Field(True, description="Include CTA in email")
    custom_instructions: Optional[str] = Field(None, max_length=1000, description="Additional custom instructions (max 1000 characters)")
    
    @field_validator('tone')
    @classmethod
    def validate_tone(cls, v):
        """Validate tone is one of the allowed values"""
        allowed_tones = ['professional', 'casual', 'friendly', 'formal']
        if v and v.lower() not in allowed_tones:
            raise ValueError(f'Tone must be one of: {", ".join(allowed_tones)}')
        return v.lower() if v else v
    
    @field_validator('email_length')
    @classmethod
    def validate_email_length(cls, v):
        """Validate email length is one of the allowed values"""
        allowed_lengths = ['short', 'medium', 'long']
        if v and v.lower() not in allowed_lengths:
            raise ValueError(f'Email length must be one of: {", ".join(allowed_lengths)}')
        return v.lower() if v else v


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
    sequence_step: Optional[int] = Field(1, ge=1, le=10, description="Step number in email sequence (1-10)")
    previous_email_context: Optional[str] = Field(None, max_length=2000, description="Context from previous email (max 2000 characters)")
    campaign_id: Optional[int] = Field(None, ge=1, description="Campaign ID for saving email (must be positive)")


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
    task: Optional[str] = Field(None, max_length=500, description="Task description or instruction (max 500 characters)")
    prompt: str = Field(..., min_length=1, max_length=10000, description="Input prompt/question for the AI (1-10000 characters)")
    system_prompt: Optional[str] = Field(None, max_length=2000, description="System-level instructions (max 2000 characters)")
    temperature: Optional[float] = Field(0.7, ge=0.0, le=2.0, description="Temperature setting (0-2)")
    max_tokens: Optional[int] = Field(1000, ge=1, le=4000, description="Maximum tokens to generate (1-4000)")
    context: Optional[Dict[str, Any]] = Field(None, description="Additional context data")


class GeneralAIResponse(BaseModel):
    """Response model for general AI tasks"""
    success: bool
    output: Optional[str] = None
    error: Optional[str] = None


class ProspectCriteria(BaseModel):
    """Criteria for prospect discovery"""
    company_name: Optional[str] = Field(None, max_length=200, description="Company name (max 200 characters)")
    domain: Optional[str] = Field(None, description="Company domain")
    industry: Optional[str] = Field(None, max_length=100, description="Industry sector (max 100 characters)")
    min_size: Optional[int] = Field(None, ge=1, description="Minimum company size (employees, must be positive)")
    max_size: Optional[int] = Field(None, ge=1, description="Maximum company size (employees, must be positive)")
    min_founded_year: Optional[int] = Field(None, ge=1800, le=2100, description="Minimum founded year (1800-2100)")
    max_founded_year: Optional[int] = Field(None, ge=1800, le=2100, description="Maximum founded year (1800-2100)")
    funding_stage: Optional[str] = Field(None, max_length=50, description="Funding stage (max 50 characters)")
    annual_revenue_range: Optional[str] = Field(None, max_length=100, description="Annual revenue range (max 100 characters)")
    growth_rate: Optional[str] = Field(None, max_length=50, description="Growth rate (max 50 characters)")
    tech_used: Optional[List[str]] = Field(None, max_length=20, description="Technologies used (max 20 items)")
    tech_category: Optional[str] = Field(None, max_length=100, description="Technology category (max 100 characters)")
    target_roles: Optional[List[str]] = Field(None, max_length=20, description="Target roles (max 20 items)")
    seniority_level: Optional[str] = Field(None, max_length=50, description="Seniority level (max 50 characters)")
    headquarters_region: Optional[str] = Field(None, max_length=100, description="Headquarters region (max 100 characters)")
    hq_country: Optional[str] = Field(None, max_length=100, description="Headquarters country (max 100 characters)")
    remote_friendly: Optional[bool] = Field(None, description="Remote-friendly company")
    regions: Optional[List[str]] = Field(None, max_length=20, description="Geographic regions (max 20 items)")
    hiring_trends: Optional[str] = Field(None, max_length=200, description="Hiring trends (max 200 characters)")
    recent_tech_adoption: Optional[str] = Field(None, max_length=200, description="Recent tech adoption (max 200 characters)")
    keyword_mentions: Optional[List[str]] = Field(None, max_length=20, description="Keyword mentions (max 20 items)")
    
    @field_validator('domain')
    @classmethod
    def validate_domain(cls, v):
        """Validate domain format if provided"""
        if v is not None and v.strip():
            # Basic domain validation pattern
            domain_pattern = r'^[a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?)*\.[a-zA-Z]{2,}$'
            if not re.match(domain_pattern, v):
                raise ValueError('Invalid domain format')
        return v
    
    @field_validator('max_size')
    @classmethod
    def validate_size_range(cls, v, validation_info):
        """Validate that max_size is greater than min_size if both are provided"""
        if hasattr(validation_info, 'data') and 'min_size' in validation_info.data:
            min_size = validation_info.data.get('min_size')
            if v is not None and min_size is not None:
                if v < min_size:
                    raise ValueError('max_size must be greater than or equal to min_size')
        return v
    
    @field_validator('max_founded_year')
    @classmethod
    def validate_founded_year_range(cls, v, validation_info):
        """Validate that max_founded_year is greater than min_founded_year if both are provided"""
        if hasattr(validation_info, 'data') and 'min_founded_year' in validation_info.data:
            min_year = validation_info.data.get('min_founded_year')
            if v is not None and min_year is not None:
                if v < min_year:
                    raise ValueError('max_founded_year must be greater than or equal to min_founded_year')
        return v


class ProspectDiscoveryRequest(BaseModel):
    """Request model for prospect discovery"""
    criteria: ProspectCriteria
    max_companies: int = Field(5, ge=1, le=5, description="Maximum number of companies to return (1-5)")


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
    id: Optional[str] = Field(None, max_length=100, description="Prospect ID (max 100 characters)")
    company: str = Field(..., min_length=1, max_length=200, description="Company name (1-200 characters)")
    first_name: Optional[str] = Field(None, max_length=100, description="First name (max 100 characters)")
    last_name: Optional[str] = Field(None, max_length=100, description="Last name (max 100 characters)")
    position: Optional[str] = Field(None, max_length=200, description="Job position (max 200 characters)")
    email: Optional[str] = Field(None, description="Email address")
    domain: str = Field(..., min_length=1, description="Company domain (required)")
    industry: Optional[str] = Field(None, max_length=100, description="Industry sector (max 100 characters)")
    size: Optional[int] = Field(None, ge=1, description="Company size (employees, must be positive)")
    regions: Optional[List[str]] = Field(None, max_length=20, description="Geographic regions (max 20 items)")
    stack: Optional[List[str]] = Field(None, max_length=20, description="Technology stack (max 20 items)")
    keywords: Optional[List[str]] = Field(None, max_length=20, description="Keywords/interests (max 20 items)")
    
    @field_validator('domain')
    @classmethod
    def validate_domain(cls, v):
        """Validate domain format"""
        if v:
            domain_pattern = r'^[a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?)*\.[a-zA-Z]{2,}$'
            if not re.match(domain_pattern, v):
                raise ValueError('Invalid domain format')
        return v
    
    @field_validator('email')
    @classmethod
    def validate_email(cls, v):
        """Validate email format if provided"""
        if v is not None and v.strip():
            email_pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
            if not re.match(email_pattern, v):
                raise ValueError('Invalid email format')
        return v


class PersonalizationHookRequest(BaseModel):
    """Request model for personalization hook generation"""
    prospect: ProspectInfo
    seller: SellerProfile


class PersonalizationHookResponse(BaseModel):
    """Response model for personalization hook generation"""
    success: bool
    hook: Optional[str] = None
    error: Optional[str] = None




