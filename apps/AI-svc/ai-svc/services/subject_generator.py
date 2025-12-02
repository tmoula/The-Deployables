"""
Subject Generator Service - Generates personalized email subject lines using AI
"""
import logging
from typing import Optional, Any
from models import CompanyInfo, ContactInfo, EmailRequirements

logger = logging.getLogger(__name__)


class SubjectGenerator:
    """Service for generating personalized email subject lines"""

    def __init__(self, llm_service):
        self.llm_service = llm_service

    async def generate_subject(
        self,
        requirements: EmailRequirements,
        sequence_step: int,
        company: CompanyInfo = None,
        contact: ContactInfo = None,
        seller: Optional[Any] = None
    ) -> str:
        """Generate email subject line - MAX 50 characters with spintax"""
        system_prompt = """Generate B2B email subject lines. MAX 50 characters. Use {{company}} or {{firstName}}. Include spintax {{option1|option2|option3}}. Return subject only."""
        
        company_name = company.name if company else "company"
        company_industry = company.industry if company and company.industry else ""
        product_short = requirements.product_service_description[:20]
        
        # Very short prompt to avoid MAX_TOKENS
        prompt = f"""Subject for {company_name} ({company_industry}) about {product_short}. Use {{company}} and {{option1|option2|option3}}. Max 50 chars. Subject:"""
        
        try:
            subject = await self.llm_service.generate_text(
                prompt=prompt,
                system_prompt=system_prompt,
                temperature=0.8,
                max_tokens=40  # Reduced to avoid MAX_TOKENS
            )
            # Clean up the response
            subject = subject.strip().strip('"').strip("'").strip()
            # Ensure it's under 50 characters (truncate if needed)
            if len(subject) > 50:
                # Try to truncate at a word boundary
                truncated = subject[:47]
                last_space = truncated.rfind(' ')
                if last_space > 35:
                    subject = truncated[:last_space] + "..."
                else:
                    subject = truncated + "..."
            # Ensure spintax variables are present
            if "{{company}}" not in subject and "{{firstName}}" not in subject:
                if company_name and len(company_name) < 15 and company_name.lower() in subject.lower():
                    subject = subject.replace(company_name, "{{company}}", 1)
                else:
                    # Add company variable at the end if not present and there's room
                    if len(subject) < 40:
                        subject = subject + " - {{company}}"
            # Final check - ensure exactly 50 or less
            if len(subject) > 50:
                subject = subject[:50]
            return subject
        except Exception as e:
            logger.error(f"Error generating subject: {str(e)}")
            # Generate intelligent fallback using company/industry data
            return self._generate_fallback_subject(requirements, company, contact)

    def _generate_fallback_subject(
        self,
        requirements: EmailRequirements,
        company: CompanyInfo = None,
        contact: ContactInfo = None
    ) -> str:
        """Generate fallback subject when LLM fails - MAX 50 characters"""
        company_var = "{{company}}" if company else "your company"
        company_industry = company.industry if company and company.industry else ""
        product_short = requirements.product_service_description[:12] if requirements.product_service_description else "solution"
        
        # Create relevant subject based on company industry and product
        # Try different combinations to stay under 50 chars
        base = "{{Quick|Quick|I noticed}}"
        
        if company_industry and len(company_industry) < 12:
            # Use industry in subject if it's short enough
            fallback = f"{base} about {company_var} {{in|from}} {company_industry[:12]}"
        elif product_short:
            # Use product
            fallback = f"{base} about {product_short} {{for|at}} {company_var}"
        else:
            fallback = f"{base} about {company_var}"
        
        # Final check - ensure exactly 50 or less
        if len(fallback) > 50:
            # Truncate intelligently
            if len(company_var) > 20:
                company_var = company_var[:15] + "..."
            fallback = f"{base} about {company_var}"
            if len(fallback) > 50:
                fallback = fallback[:50]
        return fallback

