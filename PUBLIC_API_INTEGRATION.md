# Public API Integration Guide

This guide explains how to configure and use the public API integration feature in the lead service.

## Overview

The lead service now includes a `PublicApiClient` that can fetch data from external public APIs. This is useful for:
- Enriching company data
- Finding contact information
- Validating lead information
- Getting additional company details

## Configuration

### Application Properties

Add these to `application.properties` or set as environment variables:

```properties
# Enable/disable public API integration
public.api.enabled=true

# Base URL of the public API
public.api.base.url=https://api.example.com/v1

# API key for authentication (store in secrets!)
public.api.key=your-api-key-here

# Request timeout in milliseconds
public.api.timeout=10000
```

### Kubernetes Configuration

#### ConfigMap (non-sensitive config)

Update `infra/k8s/configmap.yaml`:

```yaml
PUBLIC_API_ENABLED: "true"
PUBLIC_API_BASE_URL: "https://api.example.com/v1"
PUBLIC_API_TIMEOUT: "10000"
```

#### Secrets (sensitive API keys)

Add to Kubernetes secrets:

```bash
kubectl create secret generic outreach-secrets \
  --namespace=deps-lead-svc \
  --from-literal=PUBLIC_API_KEY=your-api-key-here \
  --dry-run=client -o yaml | kubectl apply -f -
```

Or update your `secrets.yaml` file and apply it.

### Environment Variables

For local development, create a `.env` file or set environment variables:

```bash
export PUBLIC_API_ENABLED=true
export PUBLIC_API_BASE_URL=https://api.example.com/v1
export PUBLIC_API_KEY=your-api-key-here
```

## Usage

### REST API Endpoints

#### 1. Get Company Data

```bash
GET /api/v1/public-api/company?domain=example.com

Response:
{
  "success": true,
  "data": {
    "name": "Example Inc",
    "domain": "example.com",
    "industry": "Technology",
    "employees": 500,
    ...
  }
}
```

#### 2. Get Contact Data

```bash
GET /api/v1/public-api/contact?domain=example.com&firstName=John&lastName=Doe

Response:
{
  "success": true,
  "data": {
    "email": "john.doe@example.com",
    "phone": "+1-555-1234",
    "linkedin": "https://linkedin.com/in/johndoe",
    ...
  }
}
```

#### 3. Generic GET Request

```bash
GET /api/v1/public-api/data?endpoint=/companies&domain=example.com&size=500

Response:
{
  "success": true,
  "data": {
    ...
  }
}
```

#### 4. Generic POST Request

```bash
POST /api/v1/public-api/data
Content-Type: application/json

{
  "endpoint": "/search",
  "body": {
    "query": "technology companies",
    "filters": {
      "industry": "SaaS",
      "size": "100-500"
    }
  }
}
```

#### 5. Check Status

```bash
GET /api/v1/public-api/status

Response:
{
  "enabled": true
}
```

### Programmatic Usage

Use the `PublicApiClient` in your Java code:

```java
@Autowired
private PublicApiClient publicApiClient;

// Fetch company data
Map<String, Object> companyData = publicApiClient.fetchCompanyData("example.com");

// Fetch contact data
Map<String, Object> contactData = publicApiClient.fetchContactData(
    "example.com", "John", "Doe"
);

// Generic GET request
Map<String, String> params = new HashMap<>();
params.put("domain", "example.com");
Map<String, Object> data = publicApiClient.fetchData("/companies", params);

// Generic POST request
Map<String, Object> requestBody = new HashMap<>();
requestBody.put("query", "search term");
Map<String, Object> response = publicApiClient.postData("/search", requestBody);
```

## Example Public APIs

### Clearbit API

```properties
public.api.base.url=https://company.clearbit.com/v2
public.api.key=your-clearbit-api-key
```

Example usage:
```bash
GET /api/v1/public-api/company?domain=stripe.com
```

### Hunter.io API

```properties
public.api.base.url=https://api.hunter.io/v2
public.api.key=your-hunter-api-key
```

Example usage:
```bash
GET /api/v1/public-api/contact?domain=example.com&firstName=John&lastName=Doe
```

### Apollo.io API

```properties
public.api.base.url=https://api.apollo.io/v1
public.api.key=your-apollo-api-key
```

### Generic REST API

Works with any REST API that follows standard patterns:
- GET requests with query parameters
- POST requests with JSON body
- Bearer token or API key authentication

## Security Best Practices

1. **Never commit API keys to Git**
   - Store in Kubernetes secrets
   - Use environment variables for local development
   - Add `.env` to `.gitignore`

2. **Use Secrets for Sensitive Data**
   ```bash
   kubectl create secret generic outreach-secrets \
     --from-literal=PUBLIC_API_KEY=your-key \
     --namespace=deps-lead-svc
   ```

3. **Enable HTTPS Only**
   - Always use HTTPS for public API endpoints
   - Verify SSL certificates

4. **Rate Limiting**
   - Most public APIs have rate limits
   - Implement caching to reduce API calls
   - Monitor API usage

## Error Handling

The client handles errors gracefully:
- Returns empty Map if API is disabled
- Returns empty Map on API errors
- Logs errors for debugging
- Never throws exceptions (returns empty results instead)

## Testing

### Test locally:

```bash
# Enable public API
export PUBLIC_API_ENABLED=true
export PUBLIC_API_BASE_URL=https://api.example.com/v1
export PUBLIC_API_KEY=test-key

# Start the service
cd apps/lead-svc
./gradlew bootRun

# Test endpoint
curl "http://localhost:8084/api/v1/public-api/company?domain=example.com"
```

### Test in Kubernetes:

```bash
# Update ConfigMap and Secrets
kubectl apply -f infra/k8s/configmap.yaml
kubectl apply -f infra/k8s/secrets.yaml

# Restart deployment
kubectl rollout restart deployment/lead-deployment -n deps-lead-svc

# Test via port-forward
kubectl port-forward -n deps-lead-svc svc/lead-svc 8084:8081
curl "http://localhost:8084/api/v1/public-api/status"
```

## Troubleshooting

### API not working

1. Check if enabled:
   ```bash
   curl http://localhost:8084/api/v1/public-api/status
   ```

2. Check logs:
   ```bash
   kubectl logs -n deps-lead-svc deployment/lead-deployment
   ```

3. Verify configuration:
   ```bash
   kubectl get configmap outreach-config -n deps-lead-svc -o yaml
   kubectl get secret outreach-secrets -n deps-lead-svc
   ```

### Authentication errors

- Verify API key is correct
- Check if API key has proper permissions
- Verify authentication format (Bearer token vs API key)

### Timeout errors

- Increase timeout: `PUBLIC_API_TIMEOUT=30000`
- Check network connectivity
- Verify API endpoint is accessible

## Next Steps

1. Choose a public API (Clearbit, Hunter.io, Apollo, etc.)
2. Get an API key
3. Configure in Kubernetes secrets
4. Enable in ConfigMap
5. Test using the REST endpoints
6. Integrate into your lead enrichment workflow

