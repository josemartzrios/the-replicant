-- =============================================
-- V1: Initial schema for The Replicant
-- =============================================

-- Users
CREATE TABLE users (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email                VARCHAR(255) NOT NULL UNIQUE,
    name                 VARCHAR(100) NOT NULL,
    password_hash        VARCHAR(255) NOT NULL,
    role                 VARCHAR(20)  NOT NULL,
    must_change_password BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by           UUID         REFERENCES users(id),
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ
);

-- Categories
CREATE TABLE categories (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50)  NOT NULL UNIQUE,
    slug        VARCHAR(60)  NOT NULL UNIQUE,
    description VARCHAR(200),
    created_at  TIMESTAMPTZ  NOT NULL
);

-- Tags
CREATE TABLE tags (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(50)  NOT NULL UNIQUE,
    slug       VARCHAR(60)  NOT NULL UNIQUE,
    created_at TIMESTAMPTZ  NOT NULL
);

-- Posts
CREATE TABLE posts (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    title                VARCHAR(200) NOT NULL,
    slug                 VARCHAR(250) NOT NULL UNIQUE,
    content              TEXT         NOT NULL,
    excerpt              VARCHAR(300),
    status               VARCHAR(20)  NOT NULL,
    author_id            UUID         NOT NULL REFERENCES users(id),
    category_id          UUID         REFERENCES categories(id),
    reading_time_minutes INTEGER,
    published_at         TIMESTAMPTZ,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ,
    deleted_at           TIMESTAMPTZ
);

-- Post-Tag junction
CREATE TABLE post_tags (
    post_id UUID NOT NULL REFERENCES posts(id),
    tag_id  UUID NOT NULL REFERENCES tags(id),
    PRIMARY KEY (post_id, tag_id)
);

-- Refresh Tokens
CREATE TABLE refresh_tokens (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    user_id    UUID         NOT NULL REFERENCES users(id),
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    revoked_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_refresh_token_hash    ON refresh_tokens(token_hash);
CREATE        INDEX idx_refresh_token_expires ON refresh_tokens(expires_at);
CREATE        INDEX idx_refresh_token_user    ON refresh_tokens(user_id);
