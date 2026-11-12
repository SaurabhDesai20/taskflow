import json
import os

import jwt
import psycopg2
import redis
from fastapi import Depends, FastAPI, Header, HTTPException
from prometheus_fastapi_instrumentator import Instrumentator

app = FastAPI(title="TaskFlow Reports")
Instrumentator().instrument(app).expose(app)  # exposes /metrics for Prometheus later

JWT_SECRET = os.environ["JWT_SECRET"]          # same secret as the backend, so tokens are valid here
CACHE_TTL = int(os.getenv("CACHE_TTL", "30"))  # seconds

cache = redis.Redis(
    host=os.getenv("REDIS_HOST", "localhost"),
    port=int(os.getenv("REDIS_PORT", "6379")),
    decode_responses=True,
    socket_connect_timeout=1,
)


def query(sql, params=()):
    conn = psycopg2.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=os.getenv("DB_PORT", "5432"),
        dbname=os.getenv("DB_NAME", "taskflow"),
        user=os.getenv("DB_USER", "taskflow_ro"),  # read-only DB user
        password=os.environ["DB_PASSWORD"],
        connect_timeout=3,
    )
    try:
        with conn.cursor() as cur:
            cur.execute(sql, params)
            return cur.fetchall()
    finally:
        conn.close()


def current_user(authorization: str = Header(default="")):
    if not authorization.startswith("Bearer "):
        raise HTTPException(401, "missing token")
    try:
        c = jwt.decode(authorization[7:], JWT_SECRET, algorithms=["HS256"])
    except jwt.PyJWTError:
        raise HTTPException(401, "invalid token")
    return {"id": int(c["uid"]), "role": c["role"]}


@app.get("/health")
def health():
    """Liveness: is the process up?"""
    return {"status": "ok"}


@app.get("/health/ready")
def ready():
    """Readiness: can it reach its dependencies?"""
    status = {"db": "ok", "redis": "ok"}
    try:
        query("SELECT 1")
    except Exception:
        status["db"] = "down"
    try:
        cache.ping()
    except Exception:
        status["redis"] = "down"
    if "down" in status.values():
        raise HTTPException(503, status)
    return status


@app.get("/reports/summary")
def summary(user=Depends(current_user)):
    admin = user["role"] == "ADMIN"
    key = "summary:all" if admin else f"summary:user:{user['id']}"
    try:
        hit = cache.get(key)
        if hit:
            return {**json.loads(hit), "cached": True}
    except redis.RedisError:
        pass  # cache down: fall back to the database

    sql = "SELECT status, count(*) FROM tasks" + ("" if admin else " WHERE user_id = %s") + " GROUP BY status"
    rows = query(sql, () if admin else (user["id"],))
    data = {"counts": {status: n for status, n in rows}}
    try:
        cache.setex(key, CACHE_TTL, json.dumps(data))
    except redis.RedisError:
        pass
    return {**data, "cached": False}


@app.get("/reports/per-user")
def per_user(user=Depends(current_user)):
    if user["role"] != "ADMIN":
        raise HTTPException(403, "admin only")
    rows = query(
        "SELECT u.username, count(*) FILTER (WHERE t.status = 'DONE') AS done, count(t.id) AS total "
        "FROM users u LEFT JOIN tasks t ON t.user_id = u.id GROUP BY u.username ORDER BY done DESC"
    )
    return [{"username": u, "done": d, "total": t} for u, d, t in rows]
