"""
LLM Service - Handles communication with language models
Primary: Gemini
Fallback: OpenAI (ChatGPT)
"""
import os
import logging
from typing import Dict, Optional
import openai
from openai import OpenAI
import httpx
import json

logger = logging.getLogger(__name__)


class LLMService:
    """Service for interacting with language models"""
    
    def __init__(self):
        # Gemini API configuration (PRIMARY)
        self.gemini_api_key = os.getenv("GEMINI_API_KEY")
        self.gemini_api_url = os.getenv("GEMINI_API_URL", "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
        
        # OpenAI configuration (FALLBACK)
        self.openai_api_key = os.getenv("OPENAI_API_KEY")
        self.openai_model = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
        self.openai_base_url = os.getenv("OPENAI_BASE_URL", None)
        
        # Initialize OpenAI client if API key is available (for fallback)
        self.openai_client = None
        if self.openai_api_key:
            try:
                self.openai_client = OpenAI(api_key=self.openai_api_key, base_url=self.openai_base_url)
                logger.info(f"OpenAI client initialized with model {self.openai_model} (FALLBACK)")
            except Exception as e:
                logger.warning(f"Failed to initialize OpenAI client: {str(e)}")
        
        # Log primary availability
        if self.gemini_api_key:
            logger.info("Gemini API configured (PRIMARY)")
        else:
            logger.warning("No Gemini API key found. Primary LLM will not be available.")
        
        # Warn if no providers available
        if not self.gemini_api_key and not self.openai_client:
            logger.warning("No API keys found. Using mock responses. Set GEMINI_API_KEY or OPENAI_API_KEY")
    
    async def generate_text(
        self,
        prompt: str,
        system_prompt: Optional[str] = None,
        temperature: float = 0.7,
        max_tokens: int = 1000
    ) -> str:
        """
        Generate text using the configured LLM.
        Tries Gemini first (PRIMARY), falls back to OpenAI if Gemini fails.
        
        Args:
            prompt: User prompt
            system_prompt: System prompt/instructions
            temperature: Sampling temperature (0-2)
            max_tokens: Maximum tokens to generate
        
        Returns:
            Generated text
        """
        # Priority: Gemini (PRIMARY) > OpenAI (FALLBACK) > Mock
        
        # Try Gemini first (PRIMARY)
        if self.gemini_api_key:
            try:
                logger.debug("Attempting to generate text with Gemini (PRIMARY)")
                result = await self._generate_with_gemini(prompt, system_prompt, temperature, max_tokens)
                logger.info("Successfully generated text with Gemini")
                return result
            except Exception as e:
                logger.warning(f"Gemini API failed: {str(e)}. Attempting fallback to OpenAI...")
                # Fall through to OpenAI fallback
        
        # Fallback to OpenAI
        if self.openai_client:
            try:
                logger.info("Attempting to generate text with OpenAI (FALLBACK)")
                result = await self._generate_with_openai(prompt, system_prompt, temperature, max_tokens)
                logger.info("Successfully generated text with OpenAI (fallback)")
                return result
            except Exception as e:
                logger.error(f"OpenAI API also failed: {str(e)}")
                # Fall through to mock
        
        # Last resort: Mock response
        logger.error("All AI providers failed. Using mock response.")
        return self._generate_mock_response(prompt)
    
    async def _generate_with_openai(
        self,
        prompt: str,
        system_prompt: Optional[str],
        temperature: float,
        max_tokens: int
    ) -> str:
        """Generate text using OpenAI API"""
        if not self.openai_client:
            raise Exception("OpenAI client not initialized")
        
        try:
            messages = []
            if system_prompt:
                messages.append({"role": "system", "content": system_prompt})
            messages.append({"role": "user", "content": prompt})
            
            response = self.openai_client.chat.completions.create(
                model=self.openai_model,
                messages=messages,
                temperature=temperature,
                max_tokens=max_tokens
            )
            
            if not response.choices or not response.choices[0].message.content:
                raise Exception("OpenAI returned empty response")
            
            return response.choices[0].message.content.strip()
        
        except Exception as e:
            logger.error(f"OpenAI API error: {str(e)}")
            raise
    
    async def _generate_with_gemini(
        self,
        prompt: str,
        system_prompt: Optional[str],
        temperature: float,
        max_tokens: int
    ) -> str:
        """Generate text using Gemini API"""
        try:
            # Combine system prompt and user prompt
            full_prompt = prompt
            if system_prompt:
                full_prompt = f"{system_prompt}\n\n{prompt}"
            
            # Build request body for Gemini API
            request_body = {
                "contents": [{
                    "parts": [{
                        "text": full_prompt
                    }]
                }],
                "generationConfig": {
                    "temperature": temperature,
                    "maxOutputTokens": max_tokens
                }
            }
            
            # Build URL with API key
            url = f"{self.gemini_api_url}?key={self.gemini_api_key}"
            
            async with httpx.AsyncClient() as client:
                response = await client.post(
                    url,
                    json=request_body,
                    headers={"Content-Type": "application/json"},
                    timeout=60.0
                )
                response.raise_for_status()
                result = response.json()
                
                # Log full response for debugging (first 1000 chars)
                logger.info(f"Gemini API response structure: candidates={bool(result.get('candidates'))}, keys={list(result.keys())[:10]}")
                if result.get("candidates"):
                    logger.info(f"First candidate keys: {list(result['candidates'][0].keys()) if result['candidates'] else 'none'}")
                logger.debug(f"Gemini API full response: {str(result)[:1000]}")
                
                # Extract text from Gemini response
                if "error" in result:
                    error_msg = result["error"].get("message", "Unknown error")
                    logger.error(f"Gemini API error: {error_msg}")
                    logger.error(f"Full error response: {result}")
                    raise Exception(f"Gemini API Error: {error_msg}")
                
                candidates = result.get("candidates", [])
                if not candidates:
                    # Check for safety filters or blocked content
                    if "promptFeedback" in result:
                        feedback = result.get("promptFeedback", {})
                        block_reason = feedback.get("blockReason", "unknown")
                        logger.warning(f"Gemini API blocked content. Reason: {block_reason}")
                        logger.warning(f"Full response: {str(result)[:500]}")
                        raise Exception(f"Gemini API blocked content: {block_reason}")
                    logger.error(f"Gemini API returned no candidates. Full response: {str(result)[:500]}")
                    raise Exception("Gemini API returned no candidates")
                
                candidate = candidates[0]
                
                # Check for finish reason (safety filters)
                finish_reason = candidate.get("finishReason", "")
                logger.info(f"Gemini API finish reason: {finish_reason}")
                
                content = candidate.get("content", {})
                logger.info(f"Content keys: {list(content.keys()) if content else 'no content'}")
                if not content:
                    logger.error(f"Gemini API candidate has no content. Candidate: {str(candidate)[:500]}")
                    raise Exception("Gemini API returned candidate with no content")
                
                parts = content.get("parts", [])
                logger.info(f"Number of parts: {len(parts)}")
                
                # Handle MAX_TOKENS case - there might still be partial content
                if finish_reason == "MAX_TOKENS":
                    logger.warning(f"=== GEMINI MAX_TOKENS DETECTED ===")
                    logger.warning(f"Full candidate structure: {str(candidate)[:1000]}")
                    logger.warning(f"Content structure: {str(content)[:500]}")
                    logger.warning(f"Parts count: {len(parts)}, Parts content: {str(parts)[:500]}")
                    
                    # Try to get any text that might be in the response
                    if parts:
                        # There is partial content, use it
                        text = parts[0].get("text", "")
                        if text:
                            logger.info(f"✅ Using partial content from MAX_TOKENS response: {text[:200]}")
                            return text.strip()
                        else:
                            logger.warning(f"Parts exist but no 'text' key. Parts[0] keys: {list(parts[0].keys()) if parts[0] else 'empty'}")
                    
                    # Try to get text directly from content
                    if "text" in content:
                        generated_text = content.get("text", "")
                        if generated_text:
                            logger.info(f"✅ Found text in content directly: {generated_text[:200]}")
                            return generated_text.strip()
                    
                    # Log the full response for debugging
                    logger.error(f"=== MAX_TOKENS DEBUG INFO ===")
                    logger.error(f"Full result keys: {list(result.keys())}")
                    logger.error(f"Candidate keys: {list(candidate.keys())}")
                    logger.error(f"Content keys: {list(content.keys()) if content else 'no content'}")
                    logger.error(f"Parts structure: {str(parts)[:1000]}")
                    
                    # If no parts and no text, try increasing max_tokens (sometimes Gemini needs more)
                    logger.warning("MAX_TOKENS with no content. Trying with increased max_tokens...")
                    if max_tokens < 4000:
                        # Try with more tokens
                        new_max_tokens = min(4000, max_tokens * 2)
                        logger.info(f"Retrying with max_tokens={new_max_tokens}")
                        return await self._generate_with_gemini(prompt, system_prompt, temperature, new_max_tokens)
                    
                    # If already at high tokens, raise exception to trigger fallback
                    raise Exception(f"Gemini API hit MAX_TOKENS with no content. Response structure: candidate={bool(candidate)}, content={bool(content)}, parts={len(parts)}")
                
                if parts:
                    logger.info(f"First part keys: {list(parts[0].keys()) if parts[0] else 'empty part'}")
                
                if not parts:
                    if finish_reason in ["SAFETY", "RECITATION", "OTHER"]:
                        logger.warning(f"Content may have been filtered. Full candidate: {str(candidate)[:500]}")
                        raise Exception(f"Gemini API blocked content: {finish_reason}")
                    logger.error(f"Gemini API content has no parts. Content: {str(content)[:500]}")
                    logger.error(f"Full candidate: {str(candidate)[:500]}")
                    raise Exception("Gemini API returned no content parts")
                
                generated_text = parts[0].get("text", "")
                logger.info(f"Generated text length: {len(generated_text)}, preview: {generated_text[:200] if generated_text else 'EMPTY'}")
                
                # If MAX_TOKENS but we have some text, use it
                if finish_reason == "MAX_TOKENS" and generated_text:
                    logger.warning(f"Gemini hit MAX_TOKENS but returned partial content: {len(generated_text)} chars")
                
                if not generated_text:
                    logger.warning(f"Gemini API returned empty text. Parts: {parts}")
                
                return generated_text.strip()
        
        except Exception as e:
            logger.error(f"Gemini API error: {str(e)}")
            raise
    
    def _generate_mock_response(self, prompt: str) -> str:
        """Generate a mock response for development/testing"""
        logger.warning("Using mock LLM response. Configure OPENAI_API_KEY or GEMINI_API_KEY for real generation.")
        return f"[MOCK RESPONSE] This is a placeholder response for: {prompt[:50]}..."
