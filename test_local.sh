#!/bin/bash

# Helper script for local testing
# Usage: ./test_local.sh [mode]
# Modes:
#   unit   - Run Gradle unit tests (default)
#   infra  - Check if Docker Compose infrastructure is ready
#   run    - Run campaign-svc locally connected to infra

MODE=${1:-unit}

CYAN='\033[0;36m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${CYAN}=== Local Testing Helper: $MODE ===${NC}"

if [ "$MODE" = "unit" ]; then
    echo -e "Running unit tests for campaign-svc..."
    cd apps/campaign-svc || exit 1
    ./gradlew test
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Unit tests passed!${NC}"
    else
        echo -e "${RED}❌ Unit tests failed.${NC}"
        exit 1
    fi

elif [ "$MODE" = "infra" ]; then
    echo -e "Checking Docker Compose infrastructure..."
    if ! docker ps > /dev/null 2>&1; then
        echo -e "${RED}Error: Docker is not running.${NC}"
        exit 1
    fi

    # Check for specific containers
    SERVICES=("outreach-postgres" "outreach-rabbitmq")
    ALL_UP=true
    for SVC in "${SERVICES[@]}"; do
        if docker ps | grep -q "$SVC"; then
            echo -e "${GREEN}✅ $SVC is running${NC}"
        else
            echo -e "${RED}❌ $SVC is NOT running${NC}"
            ALL_UP=false
        fi
    done

    if [ "$ALL_UP" = false ]; then
        echo -e "${CYAN}Tip: Run 'cd infra && docker-compose up -d postgres rabbitmq lead-svc ai-svc' to start dependencies.${NC}"
        exit 1
    else
        echo -e "${GREEN}Infrastructure looks good!${NC}"
    fi

elif [ "$MODE" = "run" ]; then
    echo -e "Starting campaign-svc connected to local infrastructure..."
    
    # Check infra first
    $0 infra
    if [ $? -ne 0 ]; then
        exit 1
    fi

    cd apps/campaign-svc || exit 1
    
    export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/outreachdb
    export SPRING_DATASOURCE_USERNAME=postgres
    export SPRING_DATASOURCE_PASSWORD=postgres
    export RABBITMQ_HOST=localhost
    export RABBITMQ_PORT=5672
    export LEAD_SERVICE_URL=http://localhost:8084
    
    echo -e "${CYAN}Launching app on port 8081... (Press Ctrl+C to stop)${NC}"
    ./gradlew bootRun

else
    echo "Usage: ./test_local.sh [unit|infra|run]"
    exit 1
fi
