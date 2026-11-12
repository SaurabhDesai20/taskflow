import os

os.environ.setdefault("JWT_SECRET", "x" * 32)
os.environ.setdefault("DB_PASSWORD", "x")

import jwt
import main
from fastapi.testclient import TestClient

client = TestClient(main.app)


def auth(role="USER"):
    t = jwt.encode({"uid": 1, "role": role, "sub": "alice"}, os.environ["JWT_SECRET"], algorithm="HS256")
    return {"Authorization": f"Bearer {t}"}


def test_health():
    assert client.get("/health").json() == {"status": "ok"}


def test_summary_requires_token():
    assert client.get("/reports/summary").status_code == 401


def test_summary_counts(monkeypatch):
    monkeypatch.setattr(main, "query", lambda sql, params=(): [("DONE", 2), ("PENDING", 1)])
    monkeypatch.setattr(main.cache, "get", lambda k: None)
    monkeypatch.setattr(main.cache, "setex", lambda *a: None)
    r = client.get("/reports/summary", headers=auth())
    assert r.json()["counts"] == {"DONE": 2, "PENDING": 1}


def test_per_user_is_admin_only():
    assert client.get("/reports/per-user", headers=auth("USER")).status_code == 403
