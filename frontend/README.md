## Outreach Frontend

This directory contains the React‑based frontend for The Deployables B2B cold outreach platform. The application provides the web user interface for managing leads, campaigns, and configuration, and is designed to integrate with the underlying microservices and AI adapter.

---

## Purpose

The frontend acts as the primary entry point for users of the platform. It is responsible for:

- Presenting a clean, modern dashboard experience.
- Providing navigation to core modules such as **Dashboard**, **Leads**, **Campaigns**, and **Settings**.
- Hosting the authentication flows (login and related screens) as backend services mature.
- Consuming REST APIs exposed by the backend microservices.

---

## Features

- **React + Vite** development workflow.
- **Tailwind CSS** for utility‑first styling.
- **Client‑side routing** (for example, React Router) for multi‑page navigation.
- **Responsive layout** with sidebar and topbar components.
- **Dockerized** for production deployment behind Nginx.

---

## Getting Started (Local Development)

### 1. Install dependencies

```bash
npm install
```

### 2. Start the development server

```bash
npm run dev
```

By default, the application is available at:

```text
http://localhost:5173
```

The app supports hot module reloading for a fast development experience.

---

## Docker‑based Deployment

The frontend is packaged using a multi‑stage Docker build (Node build stage → Nginx runtime stage).

### 1. Build the Docker image

```bash
docker build -t outreach-frontend .
```

### 2. Run the container locally

```bash
docker run -p 3000:80 outreach-frontend
```

The UI is then reachable at:

```text
http://localhost:3000
```

This serves the optimized production build from Nginx.

---

## Kubernetes Access (Cluster‑Hosted UI)

### Option 1 – Port‑forward for local testing

```bash
kubectl port-forward svc/outreach-ui-service 3000:80 -n deps-lead-svc
```

Access the UI at:

```text
http://localhost:3000
```

### Option 2 – NodePort (depending on cluster configuration)

```bash
kubectl get nodes -o wide
```

Then open in a browser (replace with the appropriate node IP):

```text
http://<node-internal-ip>:30080
```

---

## Project Structure

```text
frontend/
 ├─ src/
 │  ├─ components/
 │  │  ├─ Sidebar.jsx
 │  │  └─ Topbar.jsx
 │  ├─ pages/
 │  │  ├─ Dashboard.jsx
 │  │  ├─ Leads.jsx
 │  │  ├─ Campaigns.jsx
 │  │  ├─ Settings.jsx
 │  │  └─ Login.jsx
 │  ├─ App.js
 │  └─ index.js
 ├─ Dockerfile
 ├─ package.json
 ├─ tailwind.config.js
 ├─ postcss.config.js
 └─ README.md
```

---

## Environment Configuration

The frontend can be configured to point to different backend environments using environment variables (for example, `REACT_APP_AUTH_URL`, `REACT_APP_API_URL`) defined via your build pipeline or Kubernetes ConfigMaps.

Ensure that these values are aligned with the corresponding backend services before deploying to shared environments.
