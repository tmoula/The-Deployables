"""
Email Generator Service - Generates personalized cold outreach emails using AI
"""
import logging
from typing import Dict, Optional
from models import CompanyInfo, ContactInfo, EmailRequirements

logger = logging.getLogger(__name__)


class EmailGenerator:
    """Service for generating personalized email content"""
    
    def __init__(self, llm_service):
        self.llm_service = llm_service
    
    async def generate_email(
        self,
        company: CompanyInfo,
        contact: ContactInfo,
        requirements: EmailRequirements,
        sequence_step: int = 1,
        previous_context: Optional[str] = None
    ) -> Dict[str, any]:
        """
        Generate a personalized email based on company, contact, and requirements.
        
        Args:
            company: Company information
            contact: Contact information
            requirements: User requirements for email generation
            sequence_step: Step number in email sequence (1 = initial, 2+ = follow-up)
            previous_context: Context from previous email if this is a follow-up
        
        Returns:
            Dictionary with 'subject', 'body', and 'personalization_score'
        """
        # Build context for personalization
        context = self._build_context(company, contact, requirements, sequence_step, previous_context)
        
        # Generate subject line
        subject = await self._generate_subject(context, requirements, sequence_step)
        
        # Generate email body
        body = await self._generate_body(context, requirements, sequence_step)
        
        # Calculate personalization score
        personalization_score = self._calculate_personalization_score(company, contact, requirements)
        
        return {
            "subject": subject,
            "body": body,
            "personalization_score": personalization_score
        }
    
    def _build_context(
        self,
        company: CompanyInfo,
        contact: ContactInfo,
        requirements: EmailRequirements,
        sequence_step: int,
        previous_context: Optional[str]
    ) -> str:
        """Build context string for email generation"""
        context_parts = []
        
        # Company information
        context_parts.append("COMPANY INFORMATION:")
        context_parts.append(f"  Name: {company.name}")
        if company.industry:
            context_parts.append(f"  Industry: {company.industry}")
        if company.employee_count:
            context_parts.append(f"  Company Size: {company.employee_count} employees")
        if company.location:
            context_parts.append(f"  Location: {company.location}")
        if company.tech_stack:
            context_parts.append(f"  Tech Stack: {', '.join(company.tech_stack)}")
        if company.enrichment_notes:
            context_parts.append(f"  Notes: {company.enrichment_notes}")
        
        # Contact information
        context_parts.append("\nCONTACT INFORMATION:")
        if contact.first_name:
            context_parts.append(f"  Name: {contact.first_name} {contact.last_name or ''}".strip())
        if contact.job_title:
            context_parts.append(f"  Job Title: {contact.job_title}")
        if contact.personalization_notes:
            context_parts.append(f"  Personal Notes: {contact.personalization_notes}")
        
        # Email sequence context
        context_parts.append(f"\nEMAIL SEQUENCE: Step {sequence_step}")
        if sequence_step > 1 and previous_context:
            context_parts.append(f"  Previous Email Context: {previous_context}")
        elif sequence_step == 1:
            context_parts.append("  This is the initial outreach email.")
        
        return "\n".join(context_parts)
    
    async def _generate_subject(
        self,
        context: str,
        requirements: EmailRequirements,
        sequence_step: int
    ) -> str:
        """Generate email subject line"""
        system_prompt = """You are an expert B2B cold email copywriter. Generate compelling, personalized email subject lines that:
- Are concise (under 60 characters when possible)
- Create curiosity or highlight value
- Avoid spam trigger words
- Are personalized to the recipient when possible
- For follow-ups, reference the previous email naturally

Return ONLY the subject line, no additional text."""
        
        prompt = f"""Generate a cold email subject line for a B2B outreach campaign.

{context}

PRODUCT/SERVICE: {requirements.product_service_description}
VALUE PROPOSITION: {requirements.value_proposition or 'Not specified'}
TONE: {requirements.tone}
SEQUENCE STEP: {sequence_step}

{"This is a follow-up email. Make it reference the previous outreach naturally." if sequence_step > 1 else "This is the initial outreach email."}

Subject line:"""
        
        try:
            subject = await self.llm_service.generate_text(
                prompt=prompt,
                system_prompt=system_prompt,
                temperature=0.8,
                max_tokens=100
            )
            # Clean up the response (remove quotes, extra whitespace)
            subject = subject.strip().strip('"').strip("'")
            return subject
        except Exception as e:
            logger.error(f"Error generating subject: {str(e)}")
            # Fallback subject
            return f"Quick question about {requirements.product_service_description[:30]}..."
    
    async def _generate_body(
        self,
        context: str,
        requirements: EmailRequirements,
        sequence_step: int
    ) -> str:
        """Generate email body"""
        system_prompt = """You are an expert B2B cold email copywriter. Write personalized, effective cold outreach emails that:
- Start with a personalized hook based on the company/contact information
- Clearly communicate the value proposition
- Address specific pain points when mentioned
- Are concise and scannable (use short paragraphs, bullet points when appropriate)
- Include a clear call-to-action
- Match the requested tone
- For follow-ups, acknowledge the previous email and add new value
- Avoid being overly salesy or pushy

Return the complete email body, ready to send."""
        
        # Determine length constraints
        length_guidance = {
            "short": "Keep it very brief (2-3 sentences, under 100 words)",
            "medium": "Keep it concise (3-4 paragraphs, 100-200 words)",
            "long": "Can be more detailed (4-6 paragraphs, 200-300 words)"
        }
        length_instruction = length_guidance.get(requirements.email_length, length_guidance["medium"])
        
        prompt = f"""Write a personalized B2B cold outreach email.

{context}

PRODUCT/SERVICE: {requirements.product_service_description}
VALUE PROPOSITION: {requirements.value_proposition or 'Not specified'}
TARGET PAIN POINTS: {', '.join(requirements.target_pain_points) if requirements.target_pain_points else 'Not specified'}
TONE: {requirements.tone}
LENGTH: {length_instruction}
INCLUDE CTA: {requirements.include_call_to_action}
SEQUENCE STEP: {sequence_step}

{"This is a follow-up email. Reference the previous outreach and add new value or a different angle." if sequence_step > 1 else "This is the initial outreach email."}

{"CUSTOM INSTRUCTIONS: " + requirements.custom_instructions if requirements.custom_instructions else ""}

Email body:"""
        
        try:
            body = await self.llm_service.generate_text(
                prompt=prompt,
                system_prompt=system_prompt,
                temperature=0.7,
                max_tokens=800 if requirements.email_length == "long" else 500
            )
            return body.strip()
        except Exception as e:
            logger.error(f"Error generating body: {str(e)}")
            # Fallback body
            return self._generate_fallback_email(requirements)
    
    def _generate_fallback_email(self, requirements: EmailRequirements) -> str:
        """Generate a simple fallback email if LLM fails"""
        return f"""Hi there,

I wanted to reach out because I think {requirements.product_service_description} could be valuable for your company.

{requirements.value_proposition or 'I believe this could help solve some of your challenges.'}

Would you be open to a quick conversation to explore if this makes sense for you?

Best regards"""
    
    def _calculate_personalization_score(
        self,
        company: CompanyInfo,
        contact: ContactInfo,
        requirements: EmailRequirements
    ) -> float:
        """
        Calculate a score (0-1) indicating how personalized the email can be.
        Higher score = more personalization data available.
        """
        score = 0.0
        max_score = 10.0
        
        # Company data (4 points max)
        if company.name:
            score += 0.5
        if company.industry:
            score += 0.5
        if company.employee_count:
            score += 0.5
        if company.tech_stack and len(company.tech_stack) > 0:
            score += 0.5
        if company.enrichment_notes:
            score += 1.0
        if company.location:
            score += 0.5
        if company.website:
            score += 0.5
        
        # Contact data (4 points max)
        if contact.first_name:
            score += 1.0
        if contact.job_title:
            score += 1.0
        if contact.personalization_notes:
            score += 1.5
        if contact.email:
            score += 0.5
        
        # Requirements quality (2 points max)
        if requirements.value_proposition:
            score += 0.5
        if requirements.target_pain_points and len(requirements.target_pain_points) > 0:
            score += 0.5
        if requirements.custom_instructions:
            score += 1.0
        
        return min(score / max_score, 1.0)

