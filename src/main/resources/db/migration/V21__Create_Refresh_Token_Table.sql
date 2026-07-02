-- V21: Create refresh_token table for server-side refresh token storage with reuse detection

CREATE TABLE refresh_token (
    id             BIGINT          NOT NULL AUTO_INCREMENT,
    user_id        BIGINT          NOT NULL,
    token          VARCHAR(512)    NOT NULL,
    expiry_date    DATETIME(6)     NOT NULL,
    is_revoked     TINYINT(1)      NOT NULL DEFAULT 0,

    -- When a token is used for rotation, the new child token is linked here.
    -- If a revoked parent token is presented again, we know it's a reuse attack.
    replaced_by    VARCHAR(512)    NULL DEFAULT NULL,

    created_at     DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uq_refresh_token_token (token),
    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
