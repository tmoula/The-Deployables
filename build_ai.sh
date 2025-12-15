#!/bin/bash
set -e

REGISTRY="harbor.javajon-gke.duckdns.org"
PROJECT="library"
USER='robot$library+developer'
PASS='c5d9eRvmIQlOiZCagsKZp4XAi3qwRAba'

echo "1. Logging in..."
echo "$PASS" | docker login $REGISTRY -u "$USER" --password-stdin

echo "2. Building AI Service..."
# Note: Directory is apps/AI-svc
docker build -t $REGISTRY/$PROJECT/ai-svc:v2 apps/AI-svc

echo "3. Pushing to Harbor..."
docker push $REGISTRY/$PROJECT/ai-svc:v2

echo "4. Restarting Deployment (if exists)..."
kubectl rollout restart deployment ai-deployment -n deps-lead-svc || echo "Deployment not found yet, skipping restart."

echo "✅ Done!"
