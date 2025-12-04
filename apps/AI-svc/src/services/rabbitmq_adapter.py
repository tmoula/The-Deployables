"""
RabbitMQ Adapter Service
Listens to RabbitMQ queues for email generation requests and processes them.
Acts as an adapter between microservices and the AI email generation service.
"""
import json
import logging
import os
import time
import threading
from typing import Dict, Optional
import pika
from pika.exceptions import AMQPConnectionError, AMQPChannelError, StreamLostError
import asyncio
from datetime import datetime

from services.email_generator import EmailGenerator
from services.llm_service import LLMService
from services.database_service import DatabaseService
from services.ai_adapter import AIAdapter
from models import CompanyInfo, ContactInfo, EmailRequirements

logger = logging.getLogger(__name__)


class RabbitMQAdapter:
    """Adapter that watches RabbitMQ for email generation requests"""
    
    def __init__(self):
        # RabbitMQ connection settings
        self.rabbitmq_host = os.getenv("RABBITMQ_HOST", "localhost")
        self.rabbitmq_port = int(os.getenv("RABBITMQ_PORT", "5672"))
        self.rabbitmq_user = os.getenv("RABBITMQ_USER", "guest")
        self.rabbitmq_password = os.getenv("RABBITMQ_PASSWORD", "guest")
        self.rabbitmq_vhost = os.getenv("RABBITMQ_VHOST", "/")
        
        # Queue names
        self.request_queue = os.getenv("AI_REQUEST_QUEUE", "ai.email.generation.requests")
        self.response_queue = os.getenv("AI_RESPONSE_QUEUE", "ai.email.generation.responses")
        self.error_queue = os.getenv("AI_ERROR_QUEUE", "ai.email.generation.errors")
        self.dlq_queue = f"{self.request_queue}.dlq"  # Dead letter queue
        
        # Retry settings
        self.max_retries = int(os.getenv("MAX_MESSAGE_RETRIES", "3"))
        self.retry_delay = int(os.getenv("RETRY_DELAY_SECONDS", "5"))
        
        # Reconnection settings
        self.reconnect_delay = 1  # Start with 1 second
        self.max_reconnect_delay = 60  # Max 60 seconds
        self.reconnect_attempts = 0
        
        # Initialize AI services
        self.llm_service = LLMService()
        self.ai_adapter = AIAdapter(self.llm_service)  # AI communication bridge
        self.email_generator = EmailGenerator(self.llm_service)
        
        # Initialize database service for enriching requests
        self.db_service = DatabaseService()
        
        # Connection and channel
        self.connection = None
        self.channel = None
        self.consumer_tag = None
        
        # Control flags
        self.is_consuming = False
        self.should_stop = False
        self._lock = threading.Lock()
        
        # Shared event loop for async operations
        self.event_loop = None
        self._loop_thread = None
        
    def _setup_event_loop(self):
        """Setup a shared event loop for async operations"""
        if self.event_loop is None or self.event_loop.is_closed():
            def run_loop():
                self.event_loop = asyncio.new_event_loop()
                asyncio.set_event_loop(self.event_loop)
                self.event_loop.run_forever()
            
            self._loop_thread = threading.Thread(target=run_loop, daemon=True)
            self._loop_thread.start()
            # Wait a bit for the loop to start
            time.sleep(0.1)
    
    def connect(self):
        """Establish connection to RabbitMQ with retry logic"""
        try:
            credentials = pika.PlainCredentials(self.rabbitmq_user, self.rabbitmq_password)
            parameters = pika.ConnectionParameters(
                host=self.rabbitmq_host,
                port=self.rabbitmq_port,
                virtual_host=self.rabbitmq_vhost,
                credentials=credentials,
                heartbeat=600,
                blocked_connection_timeout=300,
                connection_attempts=3,
                retry_delay=2
            )
            
            self.connection = pika.BlockingConnection(parameters)
            self.channel = self.connection.channel()
            
            # Declare queues with dead letter exchange
            # Request queue with DLQ support
            self.channel.queue_declare(
                queue=self.request_queue,
                durable=True,
                arguments={
                    'x-dead-letter-exchange': '',
                    'x-dead-letter-routing-key': self.dlq_queue,
                    'x-message-ttl': 3600000  # 1 hour TTL
                }
            )
            
            # Response queue
            self.channel.queue_declare(queue=self.response_queue, durable=True)
            
            # Error queue
            self.channel.queue_declare(queue=self.error_queue, durable=True)
            
            # Dead letter queue
            self.channel.queue_declare(queue=self.dlq_queue, durable=True)
            
            # Reset reconnect delay on successful connection
            self.reconnect_delay = 1
            self.reconnect_attempts = 0
            
            logger.info(f"Connected to RabbitMQ at {self.rabbitmq_host}:{self.rabbitmq_port}")
            logger.info(f"Listening on queue: {self.request_queue}")
            logger.info(f"Publishing responses to: {self.response_queue}")
            logger.info(f"Dead letter queue: {self.dlq_queue}")
            
            return True
            
        except (AMQPConnectionError, StreamLostError) as e:
            logger.error(f"Failed to connect to RabbitMQ: {str(e)}")
            return False
        except Exception as e:
            logger.error(f"Unexpected error connecting to RabbitMQ: {str(e)}")
            return False
    
    def disconnect(self):
        """Close RabbitMQ connection gracefully"""
        self.should_stop = True
        
        with self._lock:
            if self.channel and self.channel.is_open:
                try:
                    if self.consumer_tag:
                        self.channel.basic_cancel(self.consumer_tag)
                        logger.info("Cancelled consumer")
                except Exception as e:
                    logger.warning(f"Error cancelling consumer: {str(e)}")
            
            if self.connection and not self.connection.is_closed:
                try:
                    self.connection.close()
                    logger.info("Disconnected from RabbitMQ")
                except Exception as e:
                    logger.warning(f"Error closing connection: {str(e)}")
        
        # Stop event loop
        if self.event_loop and not self.event_loop.is_closed():
            self.event_loop.call_soon_threadsafe(self.event_loop.stop)
    
    def _enrich_with_database(self, company_data: Dict, contact_data: Dict, requirements_data: Dict) -> Dict:
        """Enrich request data with database information if IDs are provided"""
        enriched_company = company_data.copy()
        enriched_contact = contact_data.copy()
        enriched_requirements = requirements_data.copy()
        
        # Enrich company data if company_id is provided
        if company_data.get("company_id"):
            try:
                db_company = self.db_service.get_company_info(company_data["company_id"])
                if db_company:
                    # Merge database data, but don't overwrite provided data
                    for key, value in db_company.items():
                        if key not in enriched_company or not enriched_company[key]:
                            enriched_company[key] = value
                    logger.debug(f"Enriched company data from database for company_id: {company_data['company_id']}")
            except Exception as e:
                logger.warning(f"Failed to enrich company data from database: {str(e)}")
        
        # Enrich contact data if contact_id is provided
        if contact_data.get("contact_id"):
            try:
                db_contact = self.db_service.get_contact_info(contact_data["contact_id"])
                if db_contact:
                    # Merge database data, but don't overwrite provided data
                    for key, value in db_contact.items():
                        if key not in enriched_contact or not enriched_contact[key]:
                            enriched_contact[key] = value
                    logger.debug(f"Enriched contact data from database for contact_id: {contact_data['contact_id']}")
            except Exception as e:
                logger.warning(f"Failed to enrich contact data from database: {str(e)}")
        
        # Enrich requirements if campaign_id is provided in requirements
        # Note: This would require extending EmailRequirements model if needed
        
        return {
            "company": enriched_company,
            "contact": enriched_contact,
            "requirements": enriched_requirements
        }
    
    def _parse_request(self, message_body: str) -> Optional[Dict]:
        """Parse and validate incoming request message"""
        try:
            request_data = json.loads(message_body)
            
            # Extract required fields
            request_id = request_data.get("request_id")
            company_data = request_data.get("company", {})
            contact_data = request_data.get("contact", {})
            requirements_data = request_data.get("requirements", {})
            sequence_step = request_data.get("sequence_step", 1)
            previous_context = request_data.get("previous_email_context")
            campaign_id = request_data.get("campaign_id")  # Optional campaign ID
            
            if not request_id:
                raise ValueError("Missing required field: request_id")
            if not company_data:
                raise ValueError("Missing required field: company")
            if not contact_data:
                raise ValueError("Missing required field: contact")
            if not requirements_data:
                raise ValueError("Missing required field: requirements")
            
            # Enrich data from database if IDs are provided
            enriched = self._enrich_with_database(company_data, contact_data, requirements_data)
            
            return {
                "request_id": request_id,
                "company": CompanyInfo(**enriched["company"]),
                "contact": ContactInfo(**enriched["contact"]),
                "requirements": EmailRequirements(**enriched["requirements"]),
                "sequence_step": sequence_step,
                "previous_context": previous_context,
                "campaign_id": campaign_id  # Store for saving email later
            }
            
        except json.JSONDecodeError as e:
            logger.error(f"Invalid JSON in request: {str(e)}")
            return None
        except Exception as e:
            logger.error(f"Error parsing request: {str(e)}")
            return None
    
    async def _process_general_ai_task(self, input_data: Dict) -> Dict:
        """
        Process a general AI task using the AI adapter.
        This demonstrates how the AI adapter can be used for any AI task, not just email generation.
        
        Args:
            input_data: Dictionary with task, prompt, and optional parameters
        
        Returns:
            Dictionary with success status and AI output
        """
        try:
            # Use the AI adapter to process the input
            result = await self.ai_adapter.process_input(input_data)
            return result
        except Exception as e:
            logger.error(f"Error processing general AI task: {str(e)}", exc_info=True)
            return {
                "success": False,
                "output": None,
                "error": str(e)
            }
    
    async def _process_email_generation(self, parsed_request: Dict) -> Dict:
        """Process email generation request"""
        try:
            email_result = await self.email_generator.generate_email(
                company=parsed_request["company"],
                contact=parsed_request["contact"],
                requirements=parsed_request["requirements"],
                sequence_step=parsed_request["sequence_step"],
                previous_context=parsed_request.get("previous_context")
            )
            
            # Optionally save to database if campaign_id and contact_id are available
            email_id = None
            if parsed_request.get("campaign_id") and parsed_request["contact"].contact_id:
                try:
                    email_id = self.db_service.save_generated_email({
                        "campaign_id": parsed_request["campaign_id"],
                        "contact_id": parsed_request["contact"].contact_id,
                        "sequence_step": parsed_request["sequence_step"],
                        "subject": email_result["subject"],
                        "body": email_result["body"],
                        "status": "scheduled"
                    })
                    logger.debug(f"Saved generated email to database: email_id={email_id}")
                except Exception as e:
                    logger.warning(f"Failed to save email to database: {str(e)}")
                    # Don't fail the request if database save fails
            
            return {
                "request_id": parsed_request["request_id"],
                "success": True,
                "subject": email_result["subject"],
                "body": email_result["body"],
                "personalization_score": email_result.get("personalization_score"),
                "email_id": email_id,  # Include email_id if saved
                "generated_at": datetime.now().isoformat()
            }
            
        except Exception as e:
            logger.error(f"Error generating email: {str(e)}", exc_info=True)
            return {
                "request_id": parsed_request["request_id"],
                "success": False,
                "error": str(e),
                "generated_at": datetime.now().isoformat()
            }
    
    def _publish_response(self, response: Dict, routing_key: str = None):
        """Publish response to RabbitMQ"""
        try:
            if routing_key is None:
                routing_key = self.response_queue
            
            message = json.dumps(response)
            
            self.channel.basic_publish(
                exchange='',
                routing_key=routing_key,
                body=message,
                properties=pika.BasicProperties(
                    delivery_mode=2,  # Make message persistent
                    content_type='application/json'
                )
            )
            
            logger.info(f"Published response for request_id: {response.get('request_id')}")
            
        except Exception as e:
            logger.error(f"Error publishing response: {str(e)}")
    
    def _publish_error(self, error_data: Dict):
        """Publish error to error queue"""
        try:
            message = json.dumps(error_data)
            
            self.channel.basic_publish(
                exchange='',
                routing_key=self.error_queue,
                body=message,
                properties=pika.BasicProperties(
                    delivery_mode=2,
                    content_type='application/json'
                )
            )
            
            logger.error(f"Published error for request_id: {error_data.get('request_id')}")
            
        except Exception as e:
            logger.error(f"Error publishing error message: {str(e)}")
    
    def _handle_message(self, ch, method, properties, body):
        """Handle incoming message from RabbitMQ with retry logic"""
        retry_count = properties.headers.get('x-retry-count', 0) if properties.headers else 0
        request_id = "unknown"
        
        try:
            logger.info(f"Received message (attempt {retry_count + 1}/{self.max_retries + 1}): {body.decode()[:200]}...")
            
            # Parse request
            parsed_request = self._parse_request(body.decode())
            if not parsed_request:
                error_data = {
                    "request_id": "unknown",
                    "error": "Failed to parse request",
                    "raw_message": body.decode()[:500],
                    "timestamp": datetime.now().isoformat()
                }
                self._publish_error(error_data)
                ch.basic_ack(delivery_tag=method.delivery_tag)
                return
            
            request_id = parsed_request["request_id"]
            
            # Process request asynchronously using shared event loop
            if self.event_loop is None or self.event_loop.is_closed():
                self._setup_event_loop()
            
            # Run async function in the shared event loop
            future = asyncio.run_coroutine_threadsafe(
                self._process_email_generation(parsed_request),
                self.event_loop
            )
            response = future.result(timeout=300)  # 5 minute timeout
            
            # Publish response
            if response.get("success"):
                self._publish_response(response)
                ch.basic_ack(delivery_tag=method.delivery_tag)
                logger.info(f"Successfully processed request_id: {request_id}")
            else:
                # Check if we should retry
                if retry_count < self.max_retries:
                    logger.warning(f"Processing failed for request_id: {request_id}, retrying... (attempt {retry_count + 1}/{self.max_retries})")
                    # Reject and requeue with retry count
                    ch.basic_nack(
                        delivery_tag=method.delivery_tag,
                        requeue=True
                    )
                    # Update retry count in message headers
                    properties.headers = properties.headers or {}
                    properties.headers['x-retry-count'] = retry_count + 1
                    time.sleep(self.retry_delay)
                else:
                    # Max retries reached, send to error queue and acknowledge
                    self._publish_error(response)
                    ch.basic_ack(delivery_tag=method.delivery_tag)
                    logger.error(f"Max retries reached for request_id: {request_id}, sent to error queue")
            
        except asyncio.TimeoutError:
            logger.error(f"Timeout processing request_id: {request_id}")
            error_data = {
                "request_id": request_id,
                "error": "Processing timeout",
                "timestamp": datetime.now().isoformat()
            }
            self._publish_error(error_data)
            ch.basic_ack(delivery_tag=method.delivery_tag)
            
        except Exception as e:
            logger.error(f"Error handling message for request_id: {request_id}: {str(e)}", exc_info=True)
            
            # Check if we should retry
            if retry_count < self.max_retries:
                logger.warning(f"Retrying message for request_id: {request_id} (attempt {retry_count + 1}/{self.max_retries})")
                try:
                    ch.basic_nack(
                        delivery_tag=method.delivery_tag,
                        requeue=True
                    )
                    time.sleep(self.retry_delay)
                except Exception as nack_error:
                    logger.error(f"Error nacking message: {str(nack_error)}")
                    ch.basic_ack(delivery_tag=method.delivery_tag)
            else:
                # Max retries reached, send to error queue
                error_data = {
                    "request_id": request_id,
                    "error": str(e),
                    "timestamp": datetime.now().isoformat()
                }
                self._publish_error(error_data)
                try:
                    ch.basic_ack(delivery_tag=method.delivery_tag)
                except Exception as ack_error:
                    logger.error(f"Error acknowledging message: {str(ack_error)}")
    
    def _reconnect(self):
        """Reconnect to RabbitMQ with exponential backoff"""
        while not self.should_stop:
            logger.info(f"Attempting to reconnect to RabbitMQ (attempt {self.reconnect_attempts + 1})...")
            
            if self.connect():
                logger.info("Successfully reconnected to RabbitMQ")
                return True
            
            self.reconnect_attempts += 1
            wait_time = min(self.reconnect_delay * (2 ** (self.reconnect_attempts - 1)), self.max_reconnect_delay)
            logger.info(f"Reconnection failed. Waiting {wait_time} seconds before retry...")
            time.sleep(wait_time)
        
        return False
    
    def _check_connection(self):
        """Check if connection is still alive"""
        try:
            if self.connection is None or self.connection.is_closed:
                return False
            if self.channel is None or self.channel.is_closed:
                return False
            # Try to check connection by declaring a queue (lightweight operation)
            self.channel.queue_declare(queue=self.request_queue, passive=True)
            return True
        except Exception:
            return False
    
    def start_consuming(self):
        """Start consuming messages from RabbitMQ with automatic reconnection"""
        self._setup_event_loop()
        
        while not self.should_stop:
            # Connect to RabbitMQ
            if not self.connect():
                if not self._reconnect():
                    logger.error("Failed to reconnect. Exiting.")
                    return
                continue
            
            try:
                with self._lock:
                    # Set QoS to process one message at a time
                    self.channel.basic_qos(prefetch_count=1)
                    
                    # Start consuming
                    self.consumer_tag = self.channel.basic_consume(
                        queue=self.request_queue,
                        on_message_callback=self._handle_message,
                        auto_ack=False  # Manual acknowledgment
                    )
                    
                    self.is_consuming = True
                    logger.info("AI Adapter started. Waiting for messages. To exit press CTRL+C")
                
                # Start consuming (blocking call)
                self.channel.start_consuming()
                
            except (AMQPConnectionError, StreamLostError, AMQPChannelError) as e:
                logger.error(f"Connection lost: {str(e)}")
                self.is_consuming = False
                
                # Try to reconnect
                if not self.should_stop:
                    if not self._reconnect():
                        logger.error("Failed to reconnect. Exiting.")
                        return
                    continue
                else:
                    break
                    
            except KeyboardInterrupt:
                logger.info("Stopping AI Adapter...")
                self.should_stop = True
                break
                
            except Exception as e:
                logger.error(f"Error in consumer: {str(e)}", exc_info=True)
                self.is_consuming = False
                
                # Check if connection is still alive
                if not self._check_connection():
                    logger.warning("Connection appears to be dead, attempting reconnection...")
                    if not self.should_stop:
                        if not self._reconnect():
                            logger.error("Failed to reconnect. Exiting.")
                            return
                        continue
                else:
                    # Connection is alive but something else went wrong
                    time.sleep(5)  # Wait before retrying
        
        # Clean shutdown
        logger.info("Stopping consumer...")
        with self._lock:
            if self.channel and self.channel.is_open and self.is_consuming:
                try:
                    self.channel.stop_consuming()
                except Exception as e:
                    logger.warning(f"Error stopping consumer: {str(e)}")
        
        self.disconnect()
        logger.info("AI Adapter stopped gracefully")

