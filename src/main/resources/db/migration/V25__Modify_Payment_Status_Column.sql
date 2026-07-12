-- Modify payment_status column to VARCHAR(255) to allow setting new enum values
-- without Data Truncation errors, specifically for 'SUCCESS_REQUIRES_REFUND' (23 chars)
-- which exceeds the typical VARCHAR(20) limit.
ALTER TABLE orders MODIFY COLUMN payment_status VARCHAR(255);
