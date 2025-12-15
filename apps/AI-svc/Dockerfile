FROM python:3.11-slim

WORKDIR /app

# Install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY src/ ./src/

# Set working directory to app root so imports work
WORKDIR /app

# Ensure src is in python path
ENV PYTHONPATH=/app

# Expose port
EXPOSE 8090

# Run the application
CMD ["python", "src/main.py"]




