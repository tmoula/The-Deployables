#!/bin/bash
set -e

echo "🚀 Starting The Deployables K8s Quickstart..."

# 1. Check Pre-requisites
if ! command -v kubectl &> /dev/null; then
    echo "❌ kubectl could not be found. Please install Docker Desktop (with Kubernetes enabled)."
    exit 1
fi

echo "✅ kubectl found. Using context: $(kubectl config current-context)"

# 2. Install Ingress Controller (One-time setup)
echo "📦 Installing NGINX Ingress Controller..."
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/cloud/deploy.yaml
echo "⏳ Waiting for Ingress Controller to be ready (up to 2 mins)..."
kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=120s

# 3. Apply Infrastructure (Namespace, ConfigMaps, Secrets)
echo "🏗️ Applying Infrastructure..."
kubectl apply -f infra/k8s/namespace.yaml
kubectl apply -f infra/k8s/configmap.yaml
# Note: In a real team, secrets shouldn't be in the repo. 
# For this class project, ensure secrets.yaml exists or create dummy ones.
if [ -f "infra/k8s/secrets.yaml" ]; then
    kubectl apply -f infra/k8s/secrets.yaml
else
    echo "⚠️ infra/k8s/secrets.yaml not found! Creating from example..."
    cp infra/k8s/secrets.yaml.example infra/k8s/secrets.yaml
    echo "⚠️ PLEASE EDIT infra/k8s/secrets.yaml with real passwords!"
    kubectl apply -f infra/k8s/secrets.yaml
fi

# 4. Apply Deployments & Services
echo "🚀 Deploying Microservices..."
kubectl apply -f infra/k8s/postgres-deploy-k8s.yaml
kubectl apply -f infra/k8s/auth-svc-deploy-k8s.yaml
kubectl apply -f infra/k8s/lead-deploy-k8s.yaml
kubectl apply -f infra/k8s/lead-svc-k8s.yaml
kubectl apply -f infra/k8s/frontend-deploy-k8s.yaml
kubectl apply -f infra/k8s/frontend-svc-k8s.yaml

# 5. Apply Ingress
echo "🌐 Configuring Ingress (localhost access)..."
kubectl apply -f infra/k8s/ingress.yaml

echo "✅ Environment Setup Complete!"
echo "👉 Frontend: http://localhost"
echo "👉 Auth API: http://localhost/api/v1/auth"
echo "👉 Lead API: http://localhost/api/v1"
