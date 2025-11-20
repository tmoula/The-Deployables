"""
AI Email Generation Adapter Service
Acts as an adapter that watches RabbitMQ for email generation requests from microservices
and processes them using AI. This service listens to RabbitMQ queues and matches requests
with microservice requests.
"""
import os
import logging
import signal
import sys
from threading import Thread
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

from services.rabbitmq_adapter import RabbitMQAdapter

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Initialize RabbitMQ Adapter
adapter = RabbitMQAdapter()

# FastAPI app for health checks (optional, but useful for monitoring)
app = FastAPI(
    title="AI Email Generation Adapter",
    description="RabbitMQ adapter for AI email generation service",
    version="2.0.0"
)

# CORS middleware (if needed for health checks)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    is_connected = adapter.connection is not None and not adapter.connection.is_closed if adapter.connection else False
    return {
        "status": "healthy" if is_connected else "disconnected",
        "service": "ai-email-generation-adapter",
        "rabbitmq_connected": is_connected,
        "request_queue": adapter.request_queue,
        "response_queue": adapter.response_queue
    }


@app.get("/")
async def root():
    """Root endpoint"""
    return {
        "service": "AI Email Generation Adapter",
        "version": "2.0.0",
        "description": "RabbitMQ adapter that watches for email generation requests",
        "endpoints": {
            "health": "/health"
        },
        "queues": {
            "request": adapter.request_queue,
            "response": adapter.response_queue,
            "error": adapter.error_queue
        }
    }


def signal_handler(sig, frame):
    """Handle shutdown signals gracefully"""
    logger.info("Shutdown signal received. Stopping adapter...")
    adapter.disconnect()
    sys.exit(0)


def run_rabbitmq_consumer():
    """Run RabbitMQ consumer in a separate thread"""
    try:
        adapter.start_consuming()
    except Exception as e:
        logger.error(f"Error in RabbitMQ consumer: {str(e)}", exc_info=True)
        sys.exit(1)


if __name__ == "__main__":
    # Register signal handlers for graceful shutdown
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)
    
    # Start RabbitMQ consumer in a separate thread
    consumer_thread = Thread(target=run_rabbitmq_consumer, daemon=True)
    consumer_thread.start()
    
    # Start FastAPI server for health checks (optional)
    port = int(os.getenv("PORT", "8090"))
    logger.info(f"Starting health check server on port {port}")
    logger.info("AI Adapter is running. Listening for RabbitMQ messages...")
    
    uvicorn.run(app, host="0.0.0.0", port=port, log_level="info")

