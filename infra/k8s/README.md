# Kubernetes Configuration Management

This guide explains how to use ConfigMaps and Secrets for managing environment variables in the Kubernetes deployment.

## Overview

The project uses:
- **ConfigMaps** for non-sensitive configuration (service URLs, ports, database names)
- **Secrets** for sensitive data (passwords, API keys, tokens)

## Quick Start

### 1. Create the Namespace

```bash
kubectl apply -f infra/k8s/namespace.yaml
```

### 2. Create Secrets

**Option A: Using the example file (for development)**

```bash
# Copy the example file
cp infra/k8s/secrets.yaml.example infra/k8s/secrets.yaml

# Edit with your actual credentials
vim infra/k8s/secrets.yaml

# Apply to cluster
kubectl apply -f infra/k8s/secrets.yaml
```

**Option B: Using kubectl directly (recommended for production)**

```bash
kubectl create secret generic outreach-secrets \
  --namespace=deps-lead-svc \
  --from-literal=DB_USER=postgres \
  --from-literal=DB_PASSWORD=your-secure-password \
  --from-literal=POSTGRES_USER=postgres \
  --from-literal=POSTGRES_PASSWORD=your-secure-password \
  --from-literal=SPRING_DATASOURCE_USERNAME=postgres \
  --from-literal=SPRING_DATASOURCE_PASSWORD=your-secure-password \
  --from-literal=JWT_SECRET=your-jwt-secret-at-least-64-characters-long \
  --from-literal=OPENAI_API_KEY=your-openai-key \
  --from-literal=GEMINI_API_KEY=your-gemini-key \
  --from-literal=MAIL_USERNAME=your-email@gmail.com \
  --from-literal=MAIL_PASSWORD=your-app-password \
  --from-literal=RABBITMQ_USER=guest \
  --from-literal=RABBITMQ_PASSWORD=guest \
  --from-literal=RABBITMQ_DEFAULT_USER=guest \
  --from-literal=RABBITMQ_DEFAULT_PASS=guest
```

### 3. Create ConfigMap

```bash
kubectl apply -f infra/k8s/configmap.yaml
```

### 4. Apply Harbor Registry Secret

```bash
kubectl apply -f gke-harbor-secret.yaml -n deps-lead-svc
```

### 5. Deploy Services

```bash
cd infra/k8s

# Deploy database first
kubectl apply -f postgres-deploy-k8s.yaml

# Wait for postgres to be ready
kubectl wait --for=condition=ready pod -l app=postgres -n deps-lead-svc --timeout=120s

# Deploy services
kubectl apply -f auth-svc-deploy-k8s.yaml
kubectl apply -f lead-deploy-k8s.yaml
kubectl apply -f frontend-deploy-k8s.yaml
```

## Verify Deployment

### Check ConfigMap

```bash
kubectl get configmap outreach-config -n deps-lead-svc
kubectl describe configmap outreach-config -n deps-lead-svc
```

### Check Secrets

```bash
kubectl get secret outreach-secrets -n deps-lead-svc
kubectl describe secret outreach-secrets -n deps-lead-svc
```

### Check Pods

```bash
kubectl get pods -n deps-lead-svc
```

### Verify Environment Variables

```bash
# Check auth-svc environment
kubectl exec -n deps-lead-svc deployment/auth-deployment -- env | grep -E "DB_|JWT_SECRET|MAIL_"

# Check postgres environment
kubectl exec -n deps-lead-svc deployment/postgres-deployment -- env | grep POSTGRES
```

## Updating Configuration

### Update ConfigMap

```bash
# Edit the configmap.yaml file
vim infra/k8s/configmap.yaml

# Apply changes
kubectl apply -f infra/k8s/configmap.yaml

# Restart deployments to pick up changes
kubectl rollout restart deployment/auth-deployment -n deps-lead-svc
kubectl rollout restart deployment/lead-deployment -n deps-lead-svc
kubectl rollout restart deployment/frontend-deployment -n deps-lead-svc
```

### Update Secrets

```bash
# Option 1: Update via kubectl
kubectl create secret generic outreach-secrets \
  --namespace=deps-lead-svc \
  --from-literal=DB_PASSWORD=new-password \
  --dry-run=client -o yaml | kubectl apply -f -

# Option 2: Delete and recreate
kubectl delete secret outreach-secrets -n deps-lead-svc
kubectl apply -f infra/k8s/secrets.yaml

# Restart deployments
kubectl rollout restart deployment/auth-deployment -n deps-lead-svc
kubectl rollout restart deployment/postgres-deployment -n deps-lead-svc
```

## Security Best Practices

### DO NOT Commit Secrets to Git

Add to `.gitignore`:
```
infra/k8s/secrets.yaml
```

### Use External Secret Management (Production)

For production deployments, consider using:
- **Google Secret Manager** (recommended for GKE)
- **HashiCorp Vault**
- **AWS Secrets Manager**
- **Azure Key Vault**

Example with Google Secret Manager:
```bash
# Store secret in Google Secret Manager
gcloud secrets create db-password --data-file=- <<< "your-secure-password"

# Reference in Kubernetes using External Secrets Operator
# See: https://external-secrets.io/
```

### Rotate Secrets Regularly

```bash
# Generate new JWT secret
openssl rand -base64 64

# Update in Kubernetes
kubectl create secret generic outreach-secrets \
  --namespace=deps-lead-svc \
  --from-literal=JWT_SECRET=new-secret \
  --dry-run=client -o yaml | kubectl apply -f -

# Restart services
kubectl rollout restart deployment/auth-deployment -n deps-lead-svc
```

## Troubleshooting

### Pod fails to start with "secret not found"

```bash
# Check if secret exists
kubectl get secret outreach-secrets -n deps-lead-svc

# If not found, create it
kubectl apply -f infra/k8s/secrets.yaml
```

### Pod fails to start with "configmap not found"

```bash
# Check if configmap exists
kubectl get configmap outreach-config -n deps-lead-svc

# If not found, create it
kubectl apply -f infra/k8s/configmap.yaml
```

### Environment variables not loading

```bash
# Check pod events
kubectl describe pod <pod-name> -n deps-lead-svc

# Check logs
kubectl logs <pod-name> -n deps-lead-svc

# Verify secret/configmap keys match deployment references
kubectl get secret outreach-secrets -n deps-lead-svc -o yaml
kubectl get configmap outreach-config -n deps-lead-svc -o yaml
```

### ImagePullBackOff error

```bash
# Check if Harbor secret exists
kubectl get secret harbor-registry-secret -n deps-lead-svc

# If not found, apply it
kubectl apply -f gke-harbor-secret.yaml -n deps-lead-svc

# Restart deployment
kubectl rollout restart deployment/<deployment-name> -n deps-lead-svc
```

## Configuration Reference

### ConfigMap Keys

See `infra/k8s/configmap.yaml` for all available configuration keys.

### Secret Keys

See `infra/k8s/secrets.yaml.example` for all required secret keys.
