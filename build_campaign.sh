#!/bin/bash
set -e

REGISTRY="harbor.javajon-gke.duckdns.org"
PROJECT="library"
USER='robot$library+developer'
PASS='c5d9eRvmIQlOiZCagsKZp4XAi3qwRAba'

echo "1. Logging in..."
echo "$PASS" | docker login $REGISTRY -u "$USER" --password-stdin

echo "2. Building Campaign Service..."
# Build from root context to allow access to shared libs if any (though usually standard Maven/Gradle)
# But here Dockerfile is likely inside apps/campaign-svc
docker build -t $REGISTRY/$PROJECT/campaign-svc:v2 apps/campaign-svc

echo "3. Pushing to Harbor..."
docker push $REGISTRY/$PROJECT/campaign-svc:v2

echo "4. Restarting Deployment..."
kubectl rollout restart deployment campaign-deployment -n deps-lead-svc

echo "✅ Done!"
