-- ============================================================
-- V24: Rename OrderStatus and ShipmentStatus enum values
--      to match the new granular state machine design.
-- ============================================================

-- ── Expand Columns ────────────────────────────────────────────
-- Modify columns to VARCHAR(255) to allow setting new values
-- without Data Truncation errors (especially if they were ENUMs).
ALTER TABLE orders MODIFY COLUMN status VARCHAR(255);
ALTER TABLE shipments MODIFY COLUMN status VARCHAR(255);

-- ── Orders ──────────────────────────────────────────────────
-- Old PLACED + INITIATED/FAILED → PENDING_PAYMENT
-- (order created, payment not yet completed or failed)
UPDATE orders
SET status = 'PENDING_PAYMENT'
WHERE status = 'PLACED'
  AND payment_status IN ('INITIATED', 'FAILED');

-- Old PLACED + SUCCESS → CONFIRMED
-- (payment completed, shipment being prepared)
UPDATE orders
SET status = 'CONFIRMED'
WHERE status = 'PLACED'
  AND payment_status = 'SUCCESS';

-- Old SHIPPED → SHIPPED (no rename needed — already correct)
-- Old DELIVERED → DELIVERED (no rename needed)
-- Old CANCELLED → CANCELLED (no rename needed)
-- Old RETURNED → RETURNED (no rename needed)

-- ── Shipments ────────────────────────────────────────────────
-- Old FAILED → DELIVERY_FAILED (renamed for clarity)
UPDATE shipments
SET status = 'DELIVERY_FAILED'
WHERE status = 'FAILED';

-- ── Add admin_cancel_reason column ──────────────────────────
-- Stores the reason provided by an admin when cancelling a
-- confirmed (paid) order. Surfaced to user in notification email.
ALTER TABLE orders
    ADD COLUMN admin_cancel_reason VARCHAR(1000) NULL;
