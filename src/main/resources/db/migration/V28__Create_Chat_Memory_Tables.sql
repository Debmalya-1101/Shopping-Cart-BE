CREATE TABLE user_chat_context (
    user_id       BIGINT PRIMARY KEY,
    last_accessed TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    messages      TEXT,
    version       BIGINT NOT NULL DEFAULT 0
);
