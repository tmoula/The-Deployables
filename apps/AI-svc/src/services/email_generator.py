"""
Email Generator Service - Generates personalized cold outreach emails using AI
"""
import logging
from typing import Dict, Optional, Any
from models import CompanyInfo, ContactInfo, EmailRequirements
from services.subject_generator import SubjectGenerator

logger = logging.getLogger(__name__)


class EmailGenerator:
    """Service for generating personalized email content"""

    def __init__(self, llm_service):
        self.llm_service = llm_service
        self.subject_generator = SubjectGenerator(llm_service)
    
    async def generate_email(
        self,
        company: CompanyInfo,
        contact: ContactInfo,
        requirements: EmailRequirements,
        seller: Optional[Any] = None,
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
        context = self._build_context(company, contact, requirements, seller, sequence_step, previous_context)
        
        # Generate subject line using SubjectGenerator
        subject = await self.subject_generator.generate_subject(
            requirements=requirements,
            sequence_step=sequence_step,
            company=company,
            contact=contact,
            seller=seller
        )
        
        # Generate email body
        body = await self._generate_body(context, requirements, sequence_step, company, contact, seller)
        
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
        seller: Optional[Any],
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
        
        # Seller information
        if seller:
            context_parts.append("\nSELLER INFORMATION:")
            if hasattr(seller, 'company_name') or (isinstance(seller, dict) and seller.get('companyName')):
                seller_name = seller.get('companyName') if isinstance(seller, dict) else getattr(seller, 'company_name', '')
                context_parts.append(f"  Seller Company: {seller_name}")
            if hasattr(seller, 'industry') or (isinstance(seller, dict) and seller.get('industry')):
                seller_industry = seller.get('industry') if isinstance(seller, dict) else getattr(seller, 'industry', '')
                context_parts.append(f"  Seller Industry: {seller_industry}")
            if hasattr(seller, 'value_proposition_keywords') or (isinstance(seller, dict) and seller.get('valuePropositionKeywords')):
                vp_keywords = seller.get('valuePropositionKeywords') if isinstance(seller, dict) else getattr(seller, 'value_proposition_keywords', [])
                if vp_keywords:
                    context_parts.append(f"  Value Prop: {', '.join(vp_keywords) if isinstance(vp_keywords, list) else vp_keywords}")
        
        # Email sequence context
        context_parts.append(f"\nEMAIL SEQUENCE: Step {sequence_step}")
        if sequence_step > 1 and previous_context:
            context_parts.append(f"  Previous Email Context: {previous_context}")
        elif sequence_step == 1:
            context_parts.append("  This is the initial outreach email.")
        
        return "\n".join(context_parts)
    
    async def _generate_body(
        self,
        context: str,
        requirements: EmailRequirements,
        sequence_step: int,
        company: CompanyInfo = None,
        contact: ContactInfo = None,
        seller: Optional[Any] = None
    ) -> str:
        """Generate email body"""
        system_prompt = """You are a B2B cold email expert. Write personalized emails with spintax.

CRITICAL RULES:
- DOUBLE BRACES {{variable}} = CSV variables: {{firstName}}, {{lastName}}, {{company}}, {{position}}, {{personalizationHook}}, etc.
- SINGLE BRACES {option1|option2|option3} = Spintax rotation for word/phrase variations
- NEVER hardcode values - use {{variable}} from CSV data
- Example: {{I've been following {{company}}'s growth|I recently noted {{company}}'s trajectory|My team highlighted {{company}}'s expansion}}
- Be concise, include CTA, match tone
- Return email body with spintax only."""
        
        # Determine length constraints - default to short for testing
        length_guidance = {
            "short": "Keep it very brief (2-3 sentences, under 100 words)",
            "medium": "Keep it concise (3-4 paragraphs, 100-200 words)",
            "long": "Can be more detailed (4-6 paragraphs, 200-300 words)"
        }
        # Force short for testing
        length_instruction = length_guidance.get("short", length_guidance["short"])
        
        # Build detailed prompt with all context
        company_name = company.name if company else "the company"
        contact_name = f"{contact.first_name or ''} {contact.last_name or ''}".strip() if contact else ""
        company_industry = company.industry if company and company.industry else ""
        company_size = f"{company.employee_count} employees" if company and company.employee_count else ""
        enrichment = company.enrichment_notes[:150] if company and company.enrichment_notes else ""
        contact_notes = contact.personalization_notes[:100] if contact and contact.personalization_notes else ""
        
        # Get seller info
        seller_company = ""
        seller_industry = ""
        seller_value_prop = ""
        seller_keywords = []
        if seller:
            seller_company = seller.get('companyName', '') if isinstance(seller, dict) else getattr(seller, 'company_name', '')
            seller_industry = seller.get('industry', '') if isinstance(seller, dict) else getattr(seller, 'industry', '')
            seller_value_prop = requirements.value_proposition or ""
            seller_keywords = seller.get('valuePropositionKeywords', []) if isinstance(seller, dict) else getattr(seller, 'value_proposition_keywords', [])
        
        # Build prompt with proper escaping for braces
        contact_title = contact.job_title if contact and contact.job_title else 'Decision Maker'
        enrichment_short = enrichment[:50] if enrichment else company_industry
        
        # Very short prompt to avoid MAX_TOKENS - use regular string to avoid f-string brace issues
        product_short = requirements.product_service_description[:40] if requirements.product_service_description else "solution"
        value_short = (seller_value_prop or requirements.value_proposition or 'Help')[:30] if (seller_value_prop or requirements.value_proposition) else 'Help'
        
        prompt = """Short B2B email with spintax.

Use {{firstName}}, {{company}}, {{position}} for variables.
Use {option1|option2} for spintax.

Example: Hi {{firstName}}, {I noticed {{company}}|I saw {{company}}}. Our """ + product_short + """ helps. """ + value_short + """. CTA?

Email:"""
        
        try:
            body = await self.llm_service.generate_text(
                prompt=prompt,
                system_prompt=system_prompt,
                temperature=0.8,  # Higher temperature for more creative/varied spintax
                max_tokens=300  # Reduced for short emails during testing
            )
            # Ensure CSV variables use double braces and fix if needed
            body = body.strip()
            
            # Fix single braces to double braces for CSV variable names
            # CSV variables: company, firstName, lastName, position, personalizationHook, industry, email
            # These should ALWAYS use double braces {{variable}}
            # Spintax uses single braces {option1|option2|option3}
            import re
            
            csv_variables = ['company', 'firstName', 'lastName', 'position', 'personalizationHook', 'industry', 'email']
            
            # Replace ALL instances of {variableName} with {{variableName}}
            # This is safe because CSV variable names are specific and won't conflict with spintax
            for var in csv_variables:
                # Replace {variable} with {{variable}} - simple and direct
                # Use word boundary to ensure we match the exact variable name
                pattern = r'\{' + re.escape(var) + r'\}'
                replacement = '{{' + var + '}}'
                body = re.sub(pattern, replacement, body)
            
            logger.info(f"After variable replacement - Has {{company}}: {'{{company}}' in body}, Has {{firstName}}: {'{{firstName}}' in body}")
            
            # If no spintax variables found, add them
            if "{{firstName}}" not in body and "{{company}}" not in body:
                logger.warning("Generated email missing spintax variables, adding them")
                # Try to add variables in a natural way
                if body.startswith("Hi"):
                    body = body.replace("Hi", "Hi {{firstName}}", 1)
                elif body.startswith("Hello"):
                    body = body.replace("Hello", "Hello {{firstName}}", 1)
            return body
        except Exception as e:
            logger.error(f"Error generating body: {str(e)}")
            # Fallback body with spintax
            if company and contact:
                body = self._generate_fallback_email(requirements, company, contact)
            else:
                body = self._generate_fallback_email(requirements)
            
            # Apply variable replacement to fallback email too
            import re
            csv_variables = ['company', 'firstName', 'lastName', 'position', 'personalizationHook', 'industry', 'email']
            for var in csv_variables:
                pattern = r'\{' + re.escape(var) + r'\}'
                replacement = '{{' + var + '}}'
                body = re.sub(pattern, replacement, body)
            
            return body
    
    def _generate_fallback_email(self, requirements: EmailRequirements, company: CompanyInfo = None, contact: ContactInfo = None) -> str:
        """Generate a simple fallback email with spintax if LLM fails - uses actual CSV data"""
        company_name = company.name if company else "your company"
        company_industry = company.industry if company and company.industry else "growing"
        job_title = contact.job_title if contact and contact.job_title else "Decision Maker"
        product_service_part = requirements.product_service_description if requirements.product_service_description else "our solution"
        value_proposition_part = requirements.value_proposition or 'I believe this could help solve some of your challenges.'
        
        # Use actual company name in spintax variations (not hardcoded)
        return f"""{{Hi|Hello}} {{firstName}},

{{I've been following {{company}}'s growth|I recently noted {{company}}'s impressive trajectory|My team highlighted {{company}}'s rapid expansion}} as a {{leading|fast-growing|innovative}} {company_industry} company. {{As {{position}}|Given your role as {{position}}|With your position as {{position}}}}, I understand {{you're navigating significant scaling challenges|optimizing operations is key for you|streamlining operations is a priority for you}}.

{value_proposition_part}

{{Would you be open to|Are you open to|I'd love to schedule}} a {{quick conversation|brief call|15-minute chat}} {{this week|to explore|to see}} how this could benefit {{company}}?

{{Best regards|Sincerely|Kind regards}}"""
    
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

