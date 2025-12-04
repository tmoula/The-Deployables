"""
AI Adapter - Communication bridge between system and AI models
Acts as a general-purpose adapter that receives input, processes it through AI, and returns output.
"""
import logging
from typing import Dict, Any, Optional
import asyncio

from services.llm_service import LLMService

logger = logging.getLogger(__name__)


class AIAdapter:
    """
    AI Adapter that acts as a communication bridge between the system and AI models.
    Receives input from the system, communicates the task to the AI model, and returns the output.
    """
    
    def __init__(self, model: Optional[LLMService] = None):
        """
        Initialize the AI Adapter with an AI model.
        
        Args:
            model: LLMService instance (OpenAI primary, Gemini fallback, or mock).
                  If None, creates a new LLMService instance.
        """
        if model is None:
            self.model = LLMService()
        else:
            self.model = model
        
        logger.info("AI Adapter initialized")
    
    async def process_input(self, input_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Process input data through the AI model and return the output.
        
        This is the main communication bridge method. It receives input from the system,
        sends it to the AI model, and returns the AI's response.
        
        Args:
            input_data: Dictionary containing:
                - task: The task description or instruction for the AI
                - prompt: The input prompt/question for the AI
                - system_prompt: Optional system-level instructions
                - temperature: Optional temperature setting (default: 0.7)
                - max_tokens: Optional max tokens (default: 1000)
                - context: Optional additional context data
        
        Returns:
            Dictionary containing:
                - success: Boolean indicating if processing was successful
                - output: The AI-generated response
                - error: Error message if processing failed (only if success=False)
        """
        try:
            # Extract input parameters
            task = input_data.get("task", "")
            prompt = input_data.get("prompt", "")
            system_prompt = input_data.get("system_prompt")
            temperature = input_data.get("temperature", 0.7)
            max_tokens = input_data.get("max_tokens", 1000)
            context = input_data.get("context", {})
            
            # Validate required fields
            if not prompt and not task:
                raise ValueError("Either 'prompt' or 'task' must be provided in input_data")
            
            # Build the full prompt (combine task and prompt if both provided)
            full_prompt = prompt
            if task:
                if prompt:
                    full_prompt = f"Task: {task}\n\n{prompt}"
                else:
                    full_prompt = task
            
            # Add context if provided
            if context:
                context_str = "\n".join([f"{k}: {v}" for k, v in context.items()])
                full_prompt = f"{full_prompt}\n\nContext:\n{context_str}"
            
            logger.debug(f"Processing AI task: {task[:50] if task else 'N/A'}...")
            
            # Call the AI model
            ai_response = await self.model.generate_text(
                prompt=full_prompt,
                system_prompt=system_prompt,
                temperature=temperature,
                max_tokens=max_tokens
            )
            
            logger.debug("AI processing completed successfully")
            
            return {
                "success": True,
                "output": ai_response
            }
            
        except Exception as e:
            logger.error(f"Error processing input through AI adapter: {str(e)}", exc_info=True)
            return {
                "success": False,
                "output": None,
                "error": str(e)
            }
    
    def process_input_sync(self, input_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Synchronous wrapper for process_input.
        Useful when called from synchronous code.
        
        Args:
            input_data: Same as process_input
        
        Returns:
            Same as process_input
        """
        try:
            # Try to get existing event loop
            loop = asyncio.get_event_loop()
            if loop.is_running():
                # If loop is running, we need to use run_coroutine_threadsafe
                import concurrent.futures
                future = asyncio.run_coroutine_threadsafe(
                    self.process_input(input_data),
                    loop
                )
                return future.result(timeout=300)  # 5 minute timeout
            else:
                # Loop exists but not running, we can use it
                return loop.run_until_complete(self.process_input(input_data))
        except RuntimeError:
            # No event loop, create a new one
            return asyncio.run(self.process_input(input_data))
    
    def get_model_info(self) -> Dict[str, Any]:
        """
        Get information about the underlying AI model.
        
        Returns:
            Dictionary with model information
        """
        info = {
            "model_type": "unknown",
            "model_name": "unknown",
            "available": False,
            "fallback_available": False
        }
        
        # Check for OpenAI (primary)
        if hasattr(self.model, 'openai_client') and self.model.openai_client:
            info["model_type"] = "openai"
            info["model_name"] = getattr(self.model, 'openai_model', 'unknown')
            info["available"] = True
        # Check for Gemini (fallback)
        elif hasattr(self.model, 'gemini_api_key') and self.model.gemini_api_key:
            info["model_type"] = "gemini"
            info["model_name"] = "gemini-2.5-flash"
            info["available"] = True
        
        # Check if fallback is available
        if hasattr(self.model, 'gemini_api_key') and self.model.gemini_api_key:
            info["fallback_available"] = True
        
        if not info["available"]:
            info["model_type"] = "mock"
        
        return info

