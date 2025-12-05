"""
Prospect Service - Handles prospect discovery and personalization using AI
"""
import logging
import re
from typing import List, Optional
from src.models import ProspectCriteria, SellerProfile, ProspectInfo

logger = logging.getLogger(__name__)


class ProspectService:
    """Service for prospect discovery and personalization"""
    
    def __init__(self, llm_service):
        self.llm_service = llm_service
    
    async def generate_matching_companies(self, criteria: ProspectCriteria, max_companies: int) -> List[str]:
        """
        Generate matching company domains based on criteria using AI.
        
        Args:
            criteria: Prospect search criteria
            max_companies: Maximum number of companies to return
        
        Returns:
            List of company domain names
        """
        try:
            # Build prompt for company discovery
            prompt = self._build_company_discovery_prompt(criteria, max_companies)
            
            logger.info(f"=== PROSPECT SERVICE: Generating {max_companies} matching companies ===")
            logger.info(f"Prompt length: {len(prompt)} characters")
            logger.info(f"Prompt preview: {prompt[:200]}...")
            
            # Call AI service
            # Increased to 2000 tokens to avoid MAX_TOKENS issues
            logger.info(f"Calling LLM service with max_tokens=2000")
            try:
                generated_text = await self.llm_service.generate_text(
                    prompt=prompt,
                    system_prompt="Find company domains matching criteria.",
                    temperature=0.7,
                    max_tokens=2000  # Increased to avoid MAX_TOKENS
                )
                logger.info(f"LLM raw output length: {len(generated_text) if generated_text else 0} characters")
                logger.info(f"LLM raw output preview: {generated_text[:500] if generated_text else 'EMPTY'}")
            except Exception as llm_error:
                logger.exception(f"LLM call failed: {llm_error}")
                logger.error(f"LLM error type: {type(llm_error).__name__}")
                raise  # Re-raise so we can see the error in the endpoint
            
            # Parse company domains from response
            logger.info(f"Parsing domains from LLM output...")
            domains = self._parse_company_domains(generated_text, max_companies)
            
            logger.info(f"=== PROSPECT SERVICE RESULT ===")
            logger.info(f"Generated {len(domains)} company domains: {domains}")
            return domains
            
        except Exception as e:
            logger.error(f"Error generating matching companies: {str(e)}", exc_info=True)
            logger.error(f"Exception type: {type(e).__name__}")
            return []
    
    def _build_company_discovery_prompt(self, criteria: ProspectCriteria, max_companies: int) -> str:
        """Build prompt for company discovery - simplified for Gemini"""
        prompt_parts = []
        prompt_parts.append(f"List {max_companies} real {criteria.industry or 'technology'} company domain names.\n")
        
        if criteria.min_size or criteria.max_size:
            size_str = f"{criteria.min_size or ''}-{criteria.max_size or ''}"
            prompt_parts.append(f"Company size: {size_str} employees.\n")
        
        prompt_parts.append(f"\nReturn exactly {max_companies} domains, one per line:\n")
        prompt_parts.append("example.com\n")
        prompt_parts.append("another.com\n")
        prompt_parts.append("\nOnly domain names, no explanations.")
        
        return "".join(prompt_parts)
    
    def _parse_company_domains(self, text: str, max_companies: int) -> List[str]:
        """Parse company domains from AI response"""
        domains = []
        
        if not text:
            return domains
        
        # Pattern to match domain names (using non-capturing groups to get full match)
        domain_pattern = re.compile(r"(?:[a-zA-Z0-9](?:[a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])?\.)+[a-zA-Z]{2,}")
        
        # Use finditer to get full match objects
        for match in domain_pattern.finditer(text):
            if len(domains) >= max_companies:
                break
            
            domain = match.group(0).lower().strip()
            # Remove common prefixes
            domain = re.sub(r"^(https?://)?(www\.)?", "", domain)
            # Remove trailing slashes or paths
            if "/" in domain:
                domain = domain.split("/")[0]
            
            # Validate it's a proper domain
            if "." in domain and not domain.startswith(".") and not domain.endswith("."):
                if domain not in domains:
                    domains.append(domain)
        
        logger.info(f"Parsed {len(domains)} domains from text: {text[:200]}")
        return domains[:max_companies]
    
    async def generate_personalization_hook(
        self,
        prospect: ProspectInfo,
        seller: SellerProfile
    ) -> str:
        """
        Generate a personalization hook for a prospect based on company and seller information.
        
        Args:
            prospect: Prospect information
            seller: Seller profile information
        
        Returns:
            Personalized hook string for cold outreach
        """
        try:
            # Build prompt for personalization hook
            prompt = self._build_personalization_prompt(prospect, seller)
            
            logger.info(f"Generating personalization hook for {prospect.company}")
            
            # Call AI service
            generated_text = await self.llm_service.generate_text(
                prompt=prompt,
                system_prompt="You are a sales research expert that generates highly personalized, research-based opening hooks for cold emails.",
                temperature=0.8,
                max_tokens=200
            )
            
            # Clean up the response
            hook = generated_text.strip()
            if hook.startswith('"') and hook.endswith('"'):
                hook = hook[1:-1]
            if hook.startswith("'") and hook.endswith("'"):
                hook = hook[1:-1]
            
            logger.info(f"Generated personalization hook: {hook[:50]}...")
            return hook
            
        except Exception as e:
            logger.error(f"Error generating personalization hook: {str(e)}", exc_info=True)
            # Return fallback hook
            return f"Hi {prospect.first_name or 'there'}, I noticed {prospect.company} is in {prospect.industry or 'your industry'}."
    
    def _build_personalization_prompt(self, prospect: ProspectInfo, seller: SellerProfile) -> str:
        """Build prompt for personalization hook generation"""
        prompt_parts = []
        prompt_parts.append("Generate a highly personalized, research-based opening hook (1-2 sentences, max 120 characters) for a cold email that shows deep knowledge of the prospect's company.\n\n")
        
        prompt_parts.append("SELLER INFORMATION:\n")
        prompt_parts.append(f"- Company: {seller.company_name}\n")
        prompt_parts.append(f"- Industry: {seller.industry}\n")
        if seller.value_proposition_keywords:
            prompt_parts.append(f"- Value Proposition: {', '.join(seller.value_proposition_keywords)}\n")
        if seller.target_customer_segment:
            prompt_parts.append(f"- Target Segment: {seller.target_customer_segment}\n")
        if seller.tech_stack:
            prompt_parts.append(f"- Tech Stack: {', '.join(seller.tech_stack)}\n")
        
        prompt_parts.append("\nPROSPECT INFORMATION:\n")
        if prospect.first_name and prospect.last_name:
            prompt_parts.append(f"- Name: {prospect.first_name} {prospect.last_name}\n")
        if prospect.position:
            prompt_parts.append(f"- Position: {prospect.position}\n")
        prompt_parts.append(f"- Company: {prospect.company}\n")
        prompt_parts.append(f"- Domain: {prospect.domain}\n")
        if prospect.industry:
            prompt_parts.append(f"- Industry: {prospect.industry}\n")
        if prospect.size:
            prompt_parts.append(f"- Company Size: {prospect.size} employees\n")
        if prospect.regions:
            prompt_parts.append(f"- Regions: {', '.join(prospect.regions)}\n")
        if prospect.stack:
            prompt_parts.append(f"- Tech Stack: {', '.join(prospect.stack)}\n")
        if prospect.keywords:
            prompt_parts.append(f"- Keywords/Interests: {', '.join(prospect.keywords)}\n")
        
        prompt_parts.append("\n")
        prompt_parts.append(f"Based on the company name '{prospect.company}' and domain '{prospect.domain}', generate a hook that includes:\n")
        prompt_parts.append(f"1. A SPECIFIC detail about what {prospect.company} does or offers\n")
        prompt_parts.append("2. Something that shows you researched them\n")
        prompt_parts.append(f"3. A natural connection to how {seller.company_name}'s solutions could help them\n")
        prompt_parts.append("\n")
        prompt_parts.append("Requirements:\n")
        prompt_parts.append("- Be SPECIFIC about the company's offerings or market position\n")
        prompt_parts.append("- Show research depth\n")
        prompt_parts.append("- Connect naturally to seller's value proposition\n")
        prompt_parts.append("- 1-2 sentences, max 120 characters\n")
        prompt_parts.append("- NO greetings (no 'Hi', 'Hello', etc.)\n")
        prompt_parts.append("- Sound like you actually researched the company\n")
        prompt_parts.append("\n")
        prompt_parts.append("Return ONLY the hook text, nothing else. No quotes, no explanations, no prefixes.")
        
        return "".join(prompt_parts)

