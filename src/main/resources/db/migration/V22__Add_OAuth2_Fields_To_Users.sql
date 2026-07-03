-- V22: Add OAuth2 provider fields to the users table
-- Supports Google and Facebook social login.
-- Existing email/password users default to auth_provider = 'LOCAL'.

ALTER TABLE users
    ADD COLUMN auth_provider  VARCHAR(20)   NOT NULL DEFAULT 'LOCAL'  COMMENT 'Authentication provider: LOCAL | GOOGLE | FACEBOOK',
    ADD COLUMN provider_id    VARCHAR(255)  NULL                      COMMENT 'Unique user ID from the OAuth2 provider (e.g. Google sub)',
    ADD COLUMN avatar_url     VARCHAR(1024) NULL                      COMMENT 'Profile picture URL returned by the provider',
    ADD COLUMN display_name   VARCHAR(255)  NULL                      COMMENT 'Full name returned by the provider';

-- Index to support fast lookup by provider + provider_id (used on every OAuth2 login)
CREATE INDEX idx_users_provider
    ON users (auth_provider, provider_id);
