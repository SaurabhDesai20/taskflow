# TaskFlow

A small team to-do app used as the practice target for DevOps/DevSecOps projects.

```
Browser -> React (5173) -> /api      -> Spring Boot (8080) -> Postgres
                        -> /reports  -> FastAPI (8000)     -> Postgres (read-only) + Redis
```

## Run locally (before any DevOps work)

Install: Java 17, Maven, Node 18+, Python 3.10+, PostgreSQL, Redis.

### 1. Database (run in psql as the postgres superuser)
```sql
CREATE USER taskflow_app WITH PASSWORD 'change_me_app';
CREATE USER taskflow_ro  WITH PASSWORD 'change_me_ro';
CREATE DATABASE taskflow OWNER taskflow_app;
```

### 2. Backend (creates the tables on first start via Flyway)
```bash
cd backend
export DB_PASSWORD=change_me_app
export JWT_SECRET=use-any-random-string-of-32-or-more-chars
export ADMIN_PASSWORD=admin12345
mvn spring-boot:run
```

### 3. Give the read-only user access (after step 2 created the tables)
```sql
\c taskflow
GRANT SELECT ON tasks TO taskflow_ro;
GRANT SELECT (id, username, role) ON users TO taskflow_ro;  -- no access to password hashes
```

### 4. Reports service
```bash
cd reports
python3 -m venv venv && source venv/bin/activate
pip install -r requirements.txt
export DB_PASSWORD=change_me_ro
export JWT_SECRET=<exactly the same value as the backend>
uvicorn main:app --port 8000
```

### 5. Frontend
```bash
cd frontend
npm install
npm run dev        # open http://localhost:5173
```

Log in as `admin` / your ADMIN_PASSWORD, or create an account.

## Tests
```bash
cd backend  && mvn test
cd reports  && pytest
cd frontend && npm test
```

## Useful endpoints
- Backend: `/actuator/health`, `/actuator/prometheus`
- Reports: `/health` (liveness), `/health/ready` (db + redis), `/metrics`
