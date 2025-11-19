"""
LLM Service - Handles communication with language models (OpenAI, Ollama, etc.)
"""
import os
import logging
from typing import Dict, Optional
import openai
from openai import OpenAI

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
        
        if self.use_ollama:
            logger.info(f"Using Ollama at {self.ollama_url} with model {self.ollama_model}")
        elif self.api_key:
            self.client = OpenAI(api_key=self.api_key, base_url=self.base_url)
            logger.info(f"Using OpenAI with model {self.model}")
        else:
            logger.warning("No API key found. Using mock responses. Set OPENAI_API_KEY or USE_OLLAMA=true")
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
        if self.use_ollama:
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
    
    def _generate_mock_response(self, prompt: str) -> str:
        """Generate a mock response for development/testing"""
        logger.warning("Using mock LLM response. Configure OPENAI_API_KEY or USE_OLLAMA for real generation.")
        return f"[MOCK RESPONSE] This is a placeholder response for: {prompt[:50]}..."

