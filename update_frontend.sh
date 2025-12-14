#!/bin/bash
set -e

REGISTRY="harbor.javajon-gke.duckdns.org"
PROJECT="library"
# Using your saved credentials
USER='robot$library+developer'
PASS='c5d9eRvmIQlOiZCagsKZp4XAi3qwRAba'

echo "1. Logging in..."
echo "$PASS" | docker login $REGISTRY -u "$USER" --password-stdin

echo "2. Rebuilding Frontend with RELATIVE Paths..."
# We use relative paths so NGINX on GKE handles the routing!
docker build \
  --build-arg REACT_APP_AUTH_URL=/api/v1/auth \
  --build-arg REACT_APP_API_URL=/api/v1 \
  --no-cache \
  -t $REGISTRY/$PROJECT/outreach-ui:v3 \
  frontend

echo "3. Pushing to Harbor..."
docker push $REGISTRY/$PROJECT/outreach-ui:v3

echo "4. Updating Deployment to use v3..."
# Update the deployment file to v3
sed -i '' 's|image: .*outreach-ui:.*|image: harbor.javajon-gke.duckdns.org/library/outreach-ui:v3|g' infra/k8s/frontend-deploy-k8s.yaml || sed -i 's|image: .*outreach-ui:.*|image: harbor.javajon-gke.duckdns.org/library/outreach-ui:v3|g' infra/k8s/frontend-deploy-k8s.yaml

echo "5. Restarting Pods..."
kubectl apply -f infra/k8s/frontend-deploy-k8s.yaml
kubectl rollout restart deployment frontend-deployment -n deps-lead-svc

echo "✅ Done! Reload the page in 30 seconds."
