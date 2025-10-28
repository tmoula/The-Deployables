# 🤖 Outreach AI Frontend

A cloud-native React application providing the UI for the **AI B2B Cold Outreach Agent**.  
This dashboard enables users to manage discovery, personalization, and automation of B2B outreach.

---

## 📌 Purpose

This service is the **frontend** for The Deployables’ Cloud Native SaaS project.  
It provides:

- A clean and intuitive dashboard UI
- Navigation to core modules: Leads, Campaigns, Dashboard, Settings
- Login screen for future authentication integration

Backend APIs will connect later as microservices are developed.

---

## ✅ Features

✔ React + Tailwind CSS  
✔ Sidebar navigation with icons  
✔ Multi-page routing (React Router)  
✔ Dockerized for production builds  
✔ Ready for Kubernetes deployment (future)  

---

## 🚀 Getting Started (Developer Mode)

### 1️⃣ Install dependencies

npm install

2️⃣ Start the local development server

npm start

Open in browser:
http://localhost:3000

The app hot-reloads automatically as you edit.

🐳 Docker: Production Deployment

This frontend is Dockerized using a multi-stage build (Node → NGINX).

1️⃣ Build Docker image

docker build -t outreach-ui .

2️⃣ Run container

docker run -p 3000:80 outreach-ui

Open UI in browser:
http://localhost:3000

This runs a compressed production build served by NGINX.

📂 Project Structure

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

# Option 1: Port-forward for local testing
kubectl port-forward svc/outreach-ui-service 3000:80
# Access UI at: http://localhost:3000


# Option 2: NodePort (works on some environments)
kubectl get nodes -o wide
# Then: http://<node-internal-ip>:30080
