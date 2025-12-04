"""
Database Service
Provides database connection and query capabilities for the AI adapter.
The database is in a separate container with persistent storage.
"""
import os
import logging
from typing import Optional, Dict, List
import psycopg2
from psycopg2.extras import RealDictCursor
from psycopg2.pool import SimpleConnectionPool

logger = logging.getLogger(__name__)


class DatabaseService:
    """Service for database operations"""
    
    def __init__(self):
        # Database connection settings
        self.db_host = os.getenv("DB_HOST", "localhost")
        self.db_port = int(os.getenv("DB_PORT", "5432"))
        self.db_name = os.getenv("DB_NAME", "outreachdb")
        self.db_user = os.getenv("DB_USER", "postgres")
        self.db_password = os.getenv("DB_PASSWORD", "postgres")
        
        # Connection pool
        self.pool: Optional[SimpleConnectionPool] = None
        self._initialize_pool()
    
    def _initialize_pool(self):
        """Initialize database connection pool"""
        try:
            self.pool = SimpleConnectionPool(
                minconn=1,
                maxconn=10,
                host=self.db_host,
                port=self.db_port,
                database=self.db_name,
                user=self.db_user,
                password=self.db_password
            )
            logger.info(f"Database connection pool initialized for {self.db_host}:{self.db_port}/{self.db_name}")
        except Exception as e:
            logger.error(f"Failed to initialize database connection pool: {str(e)}")
            self.pool = None
    
    def get_connection(self):
        """Get a connection from the pool"""
        if self.pool is None:
            self._initialize_pool()
        
        if self.pool is None:
            raise Exception("Database connection pool not available")
        
        return self.pool.getconn()
    
    def return_connection(self, conn):
        """Return a connection to the pool"""
        if self.pool:
            self.pool.putconn(conn)
    
    def get_company_info(self, company_id: int) -> Optional[Dict]:
        """Get company information from database"""
        conn = None
        try:
            conn = self.get_connection()
            with conn.cursor(cursor_factory=RealDictCursor) as cur:
                cur.execute(
                    """
                    SELECT company_id, name, website, industry, employee_count, 
                           hq_location as location, enrichment_notes, tech_stack
                    FROM companies
                    WHERE company_id = %s
                    """,
                    (company_id,)
                )
                result = cur.fetchone()
                if result:
                    return dict(result)
                return None
        except Exception as e:
            logger.error(f"Error fetching company info: {str(e)}")
            return None
        finally:
            if conn:
                self.return_connection(conn)
    
    def get_contact_info(self, contact_id: int) -> Optional[Dict]:
        """Get contact information from database"""
        conn = None
        try:
            conn = self.get_connection()
            with conn.cursor(cursor_factory=RealDictCursor) as cur:
                cur.execute(
                    """
                    SELECT contact_id, first_name, last_name, job_title, email, 
                           personalization_notes, company_id
                    FROM contacts
                    WHERE contact_id = %s
                    """,
                    (contact_id,)
                )
                result = cur.fetchone()
                if result:
                    return dict(result)
                return None
        except Exception as e:
            logger.error(f"Error fetching contact info: {str(e)}")
            return None
        finally:
            if conn:
                self.return_connection(conn)
    
    def get_campaign_requirements(self, campaign_id: int) -> Optional[Dict]:
        """Get campaign requirements from database"""
        conn = None
        try:
            conn = self.get_connection()
            with conn.cursor(cursor_factory=RealDictCursor) as cur:
                # This would need to be adjusted based on actual schema
                # For now, return basic campaign info
                cur.execute(
                    """
                    SELECT campaign_id, campaign_name, user_id, icp_id, status
                    FROM campaigns
                    WHERE campaign_id = %s
                    """,
                    (campaign_id,)
                )
                result = cur.fetchone()
                if result:
                    return dict(result)
                return None
        except Exception as e:
            logger.error(f"Error fetching campaign info: {str(e)}")
            return None
        finally:
            if conn:
                self.return_connection(conn)
    
    def save_generated_email(self, email_data: Dict) -> Optional[int]:
        """Save generated email to database"""
        conn = None
        try:
            conn = self.get_connection()
            with conn.cursor() as cur:
                cur.execute(
                    """
                    INSERT INTO emails (campaign_id, contact_id, sequence_step, 
                                     final_subject, final_body, status, created_at)
                    VALUES (%s, %s, %s, %s, %s, %s, NOW())
                    RETURNING email_id
                    """,
                    (
                        email_data.get("campaign_id"),
                        email_data.get("contact_id"),
                        email_data.get("sequence_step", 1),
                        email_data.get("subject"),
                        email_data.get("body"),
                        email_data.get("status", "scheduled")
                    )
                )
                email_id = cur.fetchone()[0]
                conn.commit()
                logger.info(f"Saved generated email to database: email_id={email_id}")
                return email_id
        except Exception as e:
            logger.error(f"Error saving email to database: {str(e)}")
            if conn:
                conn.rollback()
            return None
        finally:
            if conn:
                self.return_connection(conn)
    
    def close(self):
        """Close all database connections"""
        if self.pool:
            self.pool.closeall()
            logger.info("Database connection pool closed")

