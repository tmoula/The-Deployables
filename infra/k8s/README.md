## Kubernetes Configuration and Deployment

This document describes how Kubernetes resources are configured for The Deployables platform, including the use of ConfigMaps, Secrets, and supporting manifests for deploying the system to a GKE cluster.

---

## Configuration Model

The project separates configuration into:

- **ConfigMaps** for non‑sensitive configuration (for example, service URLs, ports, database names).
- **Secrets** for sensitive values (for example, passwords, API keys, tokens).

All Kubernetes manifests for the platform are located in this directory and are intended to be applied to the `deps-lead-svc` namespace in the target cluster.

---

## Quick Start

### 1. Create the namespace

```bash
kubectl apply -f infra/k8s/namespace.yaml
```

### 2. Create Secrets

**Option A – Using the example file (development and testing)**

```bash
# Copy the example file
cp infra/k8s/secrets.yaml.example infra/k8s/secrets.yaml

# Edit with your actual credentials
vim infra/k8s/secrets.yaml

# Apply to the cluster
kubectl apply -f infra/k8s/secrets.yaml
```

**Option B – Using kubectl directly (recommended for production)**

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

### 3. Create the ConfigMap

```bash
kubectl apply -f infra/k8s/configmap.yaml
```

### 4. Apply the Harbor registry secret

This secret allows the cluster to pull container images from the Harbor registry.

```bash
kubectl apply -f gke-harbor-secret.yaml -n deps-lead-svc
```

### 5. Deploy core services

```bash
cd infra/k8s

# Deploy the database first
kubectl apply -f postgres-deploy-k8s.yaml

# Wait for PostgreSQL to be ready
kubectl wait --for=condition=ready pod -l app=postgres -n deps-lead-svc --timeout=120s

# Deploy application services
kubectl apply -f auth-svc-deploy-k8s.yaml
kubectl apply -f lead-deploy-k8s.yaml
kubectl apply -f frontend-deploy-k8s.yaml
```

---

## Verifying the Deployment

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

# Check PostgreSQL environment
kubectl exec -n deps-lead-svc deployment/postgres-deployment -- env | grep POSTGRES
```

---

## Updating Configuration

### Update the ConfigMap

```bash
# Edit the ConfigMap manifest
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
# Option 1: Update via kubectl and apply in place
kubectl create secret generic outreach-secrets \
  --namespace=deps-lead-svc \
  --from-literal=DB_PASSWORD=new-password \
  --dry-run=client -o yaml | kubectl apply -f -

# Option 2: Delete and recreate from manifest
kubectl delete secret outreach-secrets -n deps-lead-svc
kubectl apply -f infra/k8s/secrets.yaml

# Restart deployments
kubectl rollout restart deployment/auth-deployment -n deps-lead-svc
kubectl rollout restart deployment/postgres-deployment -n deps-lead-svc
```

---

## Security Best Practices

### Do not commit Secrets to Git

Ensure that secret manifests are ignored by Git:

```text
infra/k8s/secrets.yaml
```

### Use external secret management in production

For production deployments, consider one of the following:

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

### Rotate secrets regularly

```bash
# Generate a new JWT secret
openssl rand -base64 64

# Update in Kubernetes
kubectl create secret generic outreach-secrets \
  --namespace=deps-lead-svc \
  --from-literal=JWT_SECRET=new-secret \
  --dry-run=client -o yaml | kubectl apply -f -

# Restart services
kubectl rollout restart deployment/auth-deployment -n deps-lead-svc
```

---

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
# Check if ConfigMap exists
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

# Verify secret/ConfigMap keys match deployment references
kubectl get secret outreach-secrets -n deps-lead-svc -o yaml
kubectl get configmap outreach-config -n deps-lead-svc -o yaml
```

### ImagePullBackOff errors

```bash
# Check if Harbor secret exists
kubectl get secret harbor-registry-secret -n deps-lead-svc

# If not found, apply it
kubectl apply -f gke-harbor-secret.yaml -n deps-lead-svc

# Restart the affected deployment
kubectl rollout restart deployment/<deployment-name> -n deps-lead-svc
```

---

## Configuration Reference

### ConfigMap keys

See `infra/k8s/configmap.yaml` for all available configuration keys and their default values.

### Secret keys

See `infra/k8s/secrets.yaml.example` for the complete list of required secret keys and their semantics.
