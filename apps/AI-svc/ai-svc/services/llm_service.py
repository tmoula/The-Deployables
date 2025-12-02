"""
LLM Service - Handles communication with language models (OpenAI, Ollama, Gemini, etc.)
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
        self.api_key = os.getenv("OPENAI_API_KEY")
        self.model = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
        self.base_url = os.getenv("OPENAI_BASE_URL", None)
        self.use_ollama = os.getenv("USE_OLLAMA", "false").lower() == "true"
        self.ollama_url = os.getenv("OLLAMA_URL", "http://localhost:11434")
        self.ollama_model = os.getenv("OLLAMA_MODEL", "llama3.2")
        
        # Gemini API configuration
        self.gemini_api_key = os.getenv("GEMINI_API_KEY")
        self.gemini_api_url = os.getenv("GEMINI_API_URL", "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
        self.use_gemini = os.getenv("USE_GEMINI", "false").lower() == "true"
        
        # Priority: Gemini > Ollama > OpenAI > Mock
        if self.use_gemini and self.gemini_api_key:
            logger.info(f"Using Gemini API with model gemini-2.5-flash")
            self.client = None
        elif self.use_ollama:
            logger.info(f"Using Ollama at {self.ollama_url} with model {self.ollama_model}")
            self.client = None
        elif self.api_key:
            self.client = OpenAI(api_key=self.api_key, base_url=self.base_url)
            logger.info(f"Using OpenAI with model {self.model}")
        else:
            logger.warning("No API key found. Using mock responses. Set OPENAI_API_KEY, GEMINI_API_KEY, or USE_OLLAMA=true")
            self.client = None
    
    async def generate_text(
        self,
        prompt: str,
        system_prompt: Optional[str] = None,
        temperature: float = 0.7,
        max_tokens: int = 1000
    ) -> str:
        """
        Generate text using the configured LLM.
        
        Args:
            prompt: User prompt
            system_prompt: System prompt/instructions
            temperature: Sampling temperature (0-2)
            max_tokens: Maximum tokens to generate
        
        Returns:
            Generated text
        """
        if self.use_gemini and self.gemini_api_key:
            return await self._generate_with_gemini(prompt, system_prompt, temperature, max_tokens)
        elif self.use_ollama:
            return await self._generate_with_ollama(prompt, system_prompt, temperature, max_tokens)
        elif self.client:
            return await self._generate_with_openai(prompt, system_prompt, temperature, max_tokens)
        else:
            # Mock response for development
            return self._generate_mock_response(prompt)
    
    async def _generate_with_openai(
        self,
        prompt: str,
        system_prompt: Optional[str],
        temperature: float,
        max_tokens: int
    ) -> str:
        """Generate text using OpenAI API"""
        try:
            messages = []
            if system_prompt:
                messages.append({"role": "system", "content": system_prompt})
            messages.append({"role": "user", "content": prompt})
            
            response = self.client.chat.completions.create(
                model=self.model,
                messages=messages,
                temperature=temperature,
                max_tokens=max_tokens
            )
            
            return response.choices[0].message.content.strip()
        
        except Exception as e:
            logger.error(f"OpenAI API error: {str(e)}")
            raise
    
    async def _generate_with_ollama(
        self,
        prompt: str,
        system_prompt: Optional[str],
        temperature: float,
        max_tokens: int
    ) -> str:
        """Generate text using Ollama API"""
        try:
            import httpx
            
            full_prompt = prompt
            if system_prompt:
                full_prompt = f"{system_prompt}\n\n{prompt}"
            
            async with httpx.AsyncClient() as client:
                response = await client.post(
                    f"{self.ollama_url}/api/generate",
                    json={
                        "model": self.ollama_model,
                        "prompt": full_prompt,
                        "stream": False,
                        "options": {
                            "temperature": temperature,
                            "num_predict": max_tokens
                        }
                    },
                    timeout=60.0
                )
                response.raise_for_status()
                result = response.json()
                return result.get("response", "").strip()
        
        except Exception as e:
            logger.error(f"Ollama API error: {str(e)}")
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
                if not parts and finish_reason == "MAX_TOKENS":
                    logger.warning(f"Gemini hit MAX_TOKENS but no parts found. Trying to increase max_tokens or simplify prompt.")
                    # Try to get any text that might be in the response
                    if "text" in content:
                        generated_text = content.get("text", "")
                        if generated_text:
                            logger.info(f"Found text in content directly: {generated_text[:200]}")
                            return generated_text.strip()
                    raise Exception("Gemini API hit MAX_TOKENS with no content. Try reducing prompt length or increasing max_tokens.")
                
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
        logger.warning("Using mock LLM response. Configure OPENAI_API_KEY, GEMINI_API_KEY, or USE_OLLAMA for real generation.")
        return f"[MOCK RESPONSE] This is a placeholder response for: {prompt[:50]}..."

