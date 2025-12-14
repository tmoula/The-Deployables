# CI/CD Testing Guide

## Required GitHub Secrets

Before testing, ensure these secrets are configured in GitHub:

1. **HARBOR_USERNAME** - `robot$library+developer`
2. **HARBOR_PASSWORD** - `c5d9eRvmIQlOiZCagsKZp4XAi3qwRAba`
3. **GCP_SA_KEY** - Google Cloud Service Account JSON key (for CD deployment)
4. **REACT_APP_AUTH_URL** (optional) - Auth service URL
5. **REACT_APP_API_URL** (optional) - API service URL

## How to Test

### Option 1: Test CI Workflow (Build & Push)

1. **Commit and push your changes:**
   ```bash
   git add .
   git commit -m "Fix CI/CD workflows - use deps project"
   git push origin main
   ```

2. **Check GitHub Actions:**
   - Go to: `https://github.com/tmoula/The-Deployables/actions`
   - You should see "CI - Build and Push to Harbor" workflow running
   - Click on it to see the progress

3. **What it does:**
   - Builds Docker images for all services
   - Pushes to: `harbor.javajon-gke.duckdns.org/deps/`
   - Tags images with commit SHA and `latest`

### Option 2: Test CD Workflow (Build & Deploy)

1. **Manual trigger (recommended for first test):**
   - Go to: `https://github.com/tmoula/The-Deployables/actions`
   - Click on "Build and Deploy" workflow
   - Click "Run workflow" button
   - Select branch: `main`
   - Click "Run workflow"

2. **Or push to main branch:**
   ```bash
   git push origin main
   ```

3. **What it does:**
   - Builds all services
   - Pushes images to Harbor
   - Deploys to GKE cluster
   - Updates K8s manifests with new image tags

## Expected Results

### CI Workflow Success:
- ✅ All 5 services build successfully
- ✅ Images pushed to Harbor registry
- ✅ Images tagged with commit SHA

### CD Workflow Success:
- ✅ All services build and push
- ✅ GKE authentication succeeds
- ✅ K8s manifests updated
- ✅ Services deployed to GKE
- ✅ Pods running in `deps-lead-svc` namespace

## Troubleshooting

### If CI fails:
- Check if `HARBOR_USERNAME` and `HARBOR_PASSWORD` secrets are set
- Verify Harbor registry URL is correct
- Check Dockerfile paths are correct

### If CD fails:
- Check if `GCP_SA_KEY` secret is set
- Verify GKE cluster name and zone are correct
- Ensure you have permissions to deploy to GKE

## Verify Images in Harbor

After CI runs, check Harbor:
- URL: `https://harbor.javajon-gke.duckdns.org`
- Project: `deps`
- You should see: `auth-svc`, `campaign-svc`, `lead-svc`, `ai-svc`, `outreach-ui`

## Verify Deployment on GKE

After CD runs:
```bash
kubectl get pods -n deps-lead-svc
kubectl get deployments -n deps-lead-svc
kubectl get services -n deps-lead-svc
```

