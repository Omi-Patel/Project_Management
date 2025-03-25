CREATE TABLE projects (
    id TEXT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    start_date BIGINT,
    end_date BIGINT,
    created_at BIGINT,
    updated_at BIGINT
);

CREATE TABLE tasks (
    id TEXT PRIMARY KEY,
    project_id TEXT REFERENCES projects(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'TO_DO',
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    created_at BIGINT,
    updated_at BIGINT
);

CREATE TABLE user_tasks (
    user_id TEXT REFERENCES users(id),
    task_id TEXT REFERENCES tasks(id),
    PRIMARY KEY (user_id, task_id),
    created_at BIGINT,
    updated_at BIGINT
);

