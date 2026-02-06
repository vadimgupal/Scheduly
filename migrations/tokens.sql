-- liquibase formatted sql

-- changeset vadimgupal:2
CREATE TABLE IF NOT EXISTS tokens
(
    user_id BIGINT PRIMARY KEY REFERENCES users(id),
    google_refresh_token TEXT NOT NULL
);
--rollback DROP TABLE IF EXISTS tokens;