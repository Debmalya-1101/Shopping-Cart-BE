ALTER TABLE delivery_partners ADD COLUMN reviewed_by_user_id BIGINT;
ALTER TABLE delivery_partners ADD COLUMN reviewed_by_username VARCHAR(100);
ALTER TABLE delivery_partners ADD COLUMN reviewed_at TIMESTAMP;
ALTER TABLE delivery_partners ADD CONSTRAINT fk_dp_reviewed_by FOREIGN KEY (reviewed_by_user_id) REFERENCES users(id) ON DELETE SET NULL;
