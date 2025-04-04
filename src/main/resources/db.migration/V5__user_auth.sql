CREATE TABLE user_auth (
    id TEXT PRIMARY KEY,
    user_id TEXT,
    email TEXT NOT NULL,
    hash_password TEXT,
    created_at BIGINT
);

CREATE TABLE refresh_tokens (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    token TEXT NOT NULL UNIQUE,
    expires_at BIGINT NOT NULL,
    created_at BIGINT,
    FOREIGN KEY (user_id) REFERENCES user_auth(id)
);