-- Migration: Add UNIQUE constraint on delivery_partners.user_id
-- A delivery partner user should only have ONE delivery_partner record.
-- The entity has @OneToOne but this was not enforced at the database level.
-- This prevents duplicate delivery_partner rows for the same user.

ALTER TABLE delivery_partners
    ADD CONSTRAINT uq_dp_user_id UNIQUE (user_id);
