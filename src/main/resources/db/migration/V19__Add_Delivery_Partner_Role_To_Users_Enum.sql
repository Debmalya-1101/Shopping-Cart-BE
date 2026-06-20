-- Migration: Add ROLE_DELIVERY_PARTNER to users.role ENUM
-- The users table was originally created with role ENUM('ROLE_USER', 'ROLE_ADMIN').
-- Delivery partner signup fails because ROLE_DELIVERY_PARTNER is not a valid ENUM value.
-- This migration extends the ENUM to include the new role.

ALTER TABLE users
    MODIFY COLUMN role ENUM('ROLE_USER', 'ROLE_ADMIN', 'ROLE_DELIVERY_PARTNER') NOT NULL DEFAULT 'ROLE_USER';
