CREATE TABLE users (
    id        BIGSERIAL PRIMARY KEY,
    username  VARCHAR(50)  UNIQUE NOT NULL,
    password  VARCHAR(100) NOT NULL,
    role      VARCHAR(20)  NOT NULL
);

CREATE TABLE tasks (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(200) NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    user_id       BIGINT       NOT NULL REFERENCES users(id),
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    completed_at  TIMESTAMP
);

CREATE INDEX idx_tasks_user ON tasks(user_id);
