# Banking App — Environment Setup Guide

A real-world multi-environment configuration reference for the `banking-app` Spring Boot project.

---

## 📁 Project Structure (Environment Files)

```
banking-app/
├── src/main/resources/
│   ├── application.properties          ← Base config (profile selector)
│   ├── application-dev.properties      ← Dev  (H2 in-memory, debug logs)
│   ├── application-stage.properties    ← Stage (MySQL via env vars)
│   └── application-prod.properties     ← Prod  (MySQL via env vars, strict)
├── .env.example                        ← ✅ Commit this (safe template)
├── .env                                ← ❌ NEVER commit (real secrets)
├── Dockerfile                          ← Multi-stage Docker build
└── docker-compose.yml                  ← Full stack (MySQL + App + Nginx)
```

---

## 🌍 Environment Profiles At a Glance

| Setting            | `dev`              | `stage`              | `prod`               |
|--------------------|--------------------|----------------------|----------------------|
| **Database**       | H2 in-memory       | MySQL (env vars)     | MySQL (env vars)     |
| **Port**           | 8080               | 8081                 | 8082                 |
| **DDL Auto**       | `create-drop`      | `update`             | `validate`           |
| **Show SQL**       | `true`             | `true`               | `false`              |
| **Log Level**      | `DEBUG`            | `INFO`               | `WARN`               |
| **H2 Console**     | ✅ Enabled         | ❌ Disabled          | ❌ Disabled          |

---

## 🚀 How to Run

### Option 1 — Local (Maven, no Docker needed)

#### Dev profile (default — uses H2, no MySQL required)
```bash
# Option A: just run, 'dev' is the default
mvn spring-boot:run

# Option B: explicit profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
> H2 console available at: http://localhost:8080/h2-console
> JDBC URL: `jdbc:h2:mem:banking_dev_db`

#### Stage profile
```bash
# Set env vars first, then run
export SPRING_PROFILES_ACTIVE=stage
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=banking_stage_db
export DB_USERNAME=root
export DB_PASSWORD=your_password

mvn spring-boot:run
```

#### Prod profile
```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_HOST=prod-db-server.com
export DB_PORT=3306
export DB_NAME=banking_prod_db
export DB_USERNAME=prod_user
export DB_PASSWORD=super_secret_prod_password

java -jar target/banking-app-0.0.1-SNAPSHOT.jar
```

> **Windows PowerShell:** Use `$env:VAR_NAME = "value"` instead of `export VAR_NAME=value`

---

### Option 2 — Docker Compose (Full Stack)

#### Step 1: Create your `.env` file
```bash
# Copy the template
cp .env.example .env

# Edit .env with your real values
# (open in any text editor)
```

#### Step 2: Fill in `.env`
```properties
SPRING_PROFILES_ACTIVE=stage
DB_HOST=mysql
DB_PORT=3306
DB_NAME=banking_stage_db
DB_USERNAME=banking_user
DB_PASSWORD=ChangeMe123!
SERVER_PORT=8081
LOG_LEVEL=INFO
JPA_DDL_AUTO=update
JPA_SHOW_SQL=true
```

#### Step 3: Start all services
```bash
# Start (reads .env automatically)
docker compose up -d

# View logs
docker compose logs -f app

# Stop all
docker compose down

# Stop and remove volumes (wipes DB data)
docker compose down -v
```

#### Use separate env files per environment
```bash
# Stage
docker compose --env-file .env.stage up -d

# Prod
docker compose --env-file .env.prod up -d
```

---

## 🔐 How Secrets Flow (The Full Picture)

```
┌─────────────────────────────────────────────────────────────────┐
│  .env file  (on developer machine or CI/CD secrets store)       │
│  DB_PASSWORD=super_secret                                       │
└──────────────┬──────────────────────────────────────────────────┘
               │  docker compose reads .env
               ▼
┌─────────────────────────────────────────────────────────────────┐
│  docker-compose.yml                                             │
│  environment:                                                   │
│    DB_PASSWORD: ${DB_PASSWORD}    ← injected from .env          │
└──────────────┬──────────────────────────────────────────────────┘
               │  passed to container as OS environment variable
               ▼
┌─────────────────────────────────────────────────────────────────┐
│  application-stage.properties                                   │
│  spring.datasource.password=${DB_PASSWORD}  ← Spring reads it  │
└─────────────────────────────────────────────────────────────────┘
```

**Key principle:** The password is NEVER written in any file that gets committed to Git.

---

## ⚠️ Production Checklist

Before going live, verify these:

- [ ] `SPRING_PROFILES_ACTIVE=prod` is set in deployment environment
- [ ] `DB_PASSWORD` is a strong, unique password (not from `.env.example`)
- [ ] `.env` / `.env.prod` is in `.gitignore` (verified: `git status` shows no `.env`)
- [ ] `spring.jpa.hibernate.ddl-auto=validate` — schema was created by a migration tool (Flyway/Liquibase)
- [ ] `spring.jpa.show-sql=false` — SQL is NOT printed in prod logs (prevents data leaks)
- [ ] Docker image runs as **non-root** user (already set in Dockerfile)
- [ ] Health check endpoint accessible: `GET /actuator/health`

---

## 🛠️ Useful Commands

```bash
# Verify .env is gitignored (should show nothing)
git status | grep .env

# Check which profile Spring loaded (look for "The following profiles are active")
docker compose logs app | grep "profiles are active"

# Inspect what env vars a running container has
docker exec banking-app env | grep -E "SPRING|DB_|SERVER"

# Build Docker image manually
docker build -t banking-app:latest .

# Run container with inline env vars (no compose)
docker run -d \
  -e SPRING_PROFILES_ACTIVE=stage \
  -e DB_HOST=localhost \
  -e DB_NAME=banking_stage_db \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=secret \
  -p 8081:8081 \
  banking-app:latest
```

---

## 📖 Key Concepts Explained

### Why `${VAR_NAME}` in properties files?
Spring Boot replaces `${VAR_NAME}` with the value of the OS environment variable `VAR_NAME` at startup.
You can provide a default: `${VAR_NAME:default_value}`.

### Why `ddl-auto=validate` in prod?
In production, Hibernate should **never** auto-modify your database schema — a wrong migration could drop columns or data. Use `validate` so the app fails fast on startup if the schema doesn't match the entities, rather than silently corrupting data.

### Why multi-stage Docker build?
The builder stage uses a full JDK (large). The runtime stage uses only a JRE (smaller, fewer attack vectors). The final image is smaller and more secure.

### Why a non-root user in Docker?
Running as root inside a container is a security risk. If the container is compromised, an attacker gets root access to the host. Non-root limits the blast radius.
