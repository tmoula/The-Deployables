# How to run the project database locally

We are using PostgreSQL as the main database for the AI B2B Cold Outreach Agent.

## 1. Prerequisites
- Docker Desktop installed
- Docker is running

## 2. Start PostgreSQL
From the root of the repo, run:

```bash
docker compose -f db/docker-compose.db.yml up -d
