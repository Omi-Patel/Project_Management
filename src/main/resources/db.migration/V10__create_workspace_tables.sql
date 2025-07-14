-- Create workspaces table
CREATE TABLE workspaces (
    id TEXT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    owner_id TEXT NOT NULL,
    created_at BIGINT,
    updated_at BIGINT,
    FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- Create workspace_members table
CREATE TABLE workspace_members (
    id TEXT PRIMARY KEY,
    workspace_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    role VARCHAR(20) DEFAULT 'MEMBER',
    joined_at BIGINT,
    updated_at BIGINT,
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE(workspace_id, user_id)
);

-- Create workspace_invitations table
CREATE TABLE workspace_invitations (
    id TEXT PRIMARY KEY,
    workspace_id TEXT NOT NULL,
    invited_email VARCHAR(100) NOT NULL,
    invited_by TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    expires_at BIGINT NOT NULL,
    created_at BIGINT,
    updated_at BIGINT,
    FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    FOREIGN KEY (invited_by) REFERENCES users(id)
);

-- Add workspace_id column to projects table
ALTER TABLE projects ADD COLUMN workspace_id TEXT;
ALTER TABLE projects ADD FOREIGN KEY (workspace_id) REFERENCES workspaces(id);

-- Insert the workspace owner as the first member when a workspace is created
-- This will be handled in the application logic 