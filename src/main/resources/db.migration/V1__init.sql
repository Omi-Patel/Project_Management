CREATE TABLE users (
    id TEXT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255),
    status VARCHAR(20),
    created_at BIGINT,
    updated_at BIGINT
);
