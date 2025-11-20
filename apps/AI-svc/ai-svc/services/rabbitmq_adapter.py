"""
RabbitMQ Adapter Service
Listens to RabbitMQ queues for email generation requests and processes them.
Acts as an adapter between microservices and the AI email generation service.
"""
import json
import logging
import os
from typing import Dict, Optional
import pika
from pika.exceptions import AMQPConnectionError
import asyncio
from datetime import datetime

from services.email_generator import EmailGenerator
from services.llm_service import LLMService
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
        
        # Initialize AI services
        self.llm_service = LLMService()
        self.email_generator = EmailGenerator(self.llm_service)
        
        # Connection and channel
        self.connection = None
        self.channel = None
        
    def connect(self):
        """Establish connection to RabbitMQ"""
        try:
            credentials = pika.PlainCredentials(self.rabbitmq_user, self.rabbitmq_password)
            parameters = pika.ConnectionParameters(
                host=self.rabbitmq_host,
                port=self.rabbitmq_port,
                virtual_host=self.rabbitmq_vhost,
                credentials=credentials,
                heartbeat=600,
                blocked_connection_timeout=300
            )
            
            self.connection = pika.BlockingConnection(parameters)
            self.channel = self.connection.channel()
            
            # Declare queues
            self.channel.queue_declare(queue=self.request_queue, durable=True)
            self.channel.queue_declare(queue=self.response_queue, durable=True)
            self.channel.queue_declare(queue=self.error_queue, durable=True)
            
            logger.info(f"Connected to RabbitMQ at {self.rabbitmq_host}:{self.rabbitmq_port}")
            logger.info(f"Listening on queue: {self.request_queue}")
            logger.info(f"Publishing responses to: {self.response_queue}")
            
            return True
            
        except AMQPConnectionError as e:
            logger.error(f"Failed to connect to RabbitMQ: {str(e)}")
            return False
        except Exception as e:
            logger.error(f"Unexpected error connecting to RabbitMQ: {str(e)}")
            return False
    
    def disconnect(self):
        """Close RabbitMQ connection"""
        if self.connection and not self.connection.is_closed:
            self.connection.close()
            logger.info("Disconnected from RabbitMQ")
    
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
            
            if not request_id:
                raise ValueError("Missing required field: request_id")
            if not company_data:
                raise ValueError("Missing required field: company")
            if not contact_data:
                raise ValueError("Missing required field: contact")
            if not requirements_data:
                raise ValueError("Missing required field: requirements")
            
            return {
                "request_id": request_id,
                "company": CompanyInfo(**company_data),
                "contact": ContactInfo(**contact_data),
                "requirements": EmailRequirements(**requirements_data),
                "sequence_step": sequence_step,
                "previous_context": previous_context
            }
            
        except json.JSONDecodeError as e:
            logger.error(f"Invalid JSON in request: {str(e)}")
            return None
        except Exception as e:
            logger.error(f"Error parsing request: {str(e)}")
            return None
    
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
            
            return {
                "request_id": parsed_request["request_id"],
                "success": True,
                "subject": email_result["subject"],
                "body": email_result["body"],
                "personalization_score": email_result.get("personalization_score"),
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
        """Handle incoming message from RabbitMQ"""
        try:
            logger.info(f"Received message: {body.decode()[:200]}...")
            
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
            
            # Process request asynchronously
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
            response = loop.run_until_complete(self._process_email_generation(parsed_request))
            loop.close()
            
            # Publish response
            if response.get("success"):
                self._publish_response(response)
            else:
                self._publish_error(response)
            
            # Acknowledge message
            ch.basic_ack(delivery_tag=method.delivery_tag)
            logger.info(f"Processed request_id: {parsed_request['request_id']}")
            
        except Exception as e:
            logger.error(f"Error handling message: {str(e)}", exc_info=True)
            # Try to acknowledge to avoid redelivery loop
            try:
                ch.basic_ack(delivery_tag=method.delivery_tag)
            except:
                pass
    
    def start_consuming(self):
        """Start consuming messages from RabbitMQ"""
        if not self.connect():
            logger.error("Failed to connect to RabbitMQ. Exiting.")
            return
        
        try:
            # Set QoS to process one message at a time
            self.channel.basic_qos(prefetch_count=1)
            
            # Start consuming
            self.channel.basic_consume(
                queue=self.request_queue,
                on_message_callback=self._handle_message
            )
            
            logger.info("AI Adapter started. Waiting for messages. To exit press CTRL+C")
            self.channel.start_consuming()
            
        except KeyboardInterrupt:
            logger.info("Stopping AI Adapter...")
            self.channel.stop_consuming()
            self.disconnect()
        except Exception as e:
            logger.error(f"Error in consumer: {str(e)}", exc_info=True)
            self.disconnect()

